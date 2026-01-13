package com.paymentservice.service;

import com.commonlibrary.constants.CouponErrorCodes;
import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.coupon.*;
import com.commonlibrary.entity.BeneficiaryType;
import com.commonlibrary.entity.PaymentMethod;
import com.commonlibrary.entity.PaymentStatus;
import com.commonlibrary.entity.PaymentType;
import com.commonlibrary.exception.BusinessException;
import com.paymentservice.dto.CouponPaymentRequestDto;
import com.paymentservice.dto.CouponPaymentResponseDto;
import com.paymentservice.entity.Payment;
import com.paymentservice.feign.AdminCouponClient;
import com.paymentservice.kafka.PaymentEventProducer;
import com.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for processing coupon-based payments.
 * Integrates with admin-service for coupon validation and redemption.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CouponPaymentService {

    private final AdminCouponClient adminCouponClient;
    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final SystemConfigurationService systemConfigService;

    /**
     * Validate a coupon for payment.
     * Called before processing payment to check coupon validity.
     */
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(
            String couponCode,
            BeneficiaryType beneficiaryType,
            Long beneficiaryId,
            Long patientId,
            Long caseId,
            BigDecimal consultationFee) {
        
        log.info("Validating coupon {} for {} {} on case {}", 
                couponCode, beneficiaryType, beneficiaryId, caseId);

        try {
            CouponValidationRequest request = CouponValidationRequest.builder()
                    .couponCode(couponCode.toUpperCase().trim())
                    .beneficiaryType(beneficiaryType)
                    .beneficiaryId(beneficiaryId)
                    .patientId(patientId)
                    .caseId(caseId)
                    .requestedAmount(consultationFee)
                    .build();

            ResponseEntity<ApiResponse<CouponValidationResponse>> response = 
                    adminCouponClient.validateCoupon(request);

            if (response.getBody() != null && response.getBody().getData() != null) {
                CouponValidationResponse validationResponse = response.getBody().getData();
                log.info("Coupon {} validation result: valid={}", couponCode, validationResponse.getValid());
                return validationResponse;
            }

            // Return invalid response if no data
            return createInvalidResponse(couponCode, "No response from coupon service", 
                    CouponErrorCodes.COUPON_VALIDATION_FAILED);

        } catch (Exception e) {
            log.error("Error validating coupon {}: {}", couponCode, e.getMessage(), e);
            return createInvalidResponse(couponCode, 
                    "Coupon validation failed: " + e.getMessage(), 
                    CouponErrorCodes.COUPON_VALIDATION_FAILED);
        }
    }

    /**
     * Process a coupon payment for a consultation.
     * Validates coupon, creates payment record, and marks coupon as used.
     */
    @Transactional
    public CouponPaymentResponseDto processCouponPayment(CouponPaymentRequestDto request) {
        log.info("Processing coupon payment for case {} with coupon {}", 
                request.getCaseId(), request.getCouponCode());

        String couponCode = request.getCouponCode().toUpperCase().trim();
        
        // Check for duplicate payment
        String idempotencyKey = generateIdempotencyKey(request);
        if (paymentRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            throw new BusinessException("Duplicate payment request", HttpStatus.CONFLICT);
        }

        // Get consultation fee
        BigDecimal consultationFee = request.getConsultationFee() != null 
                ? request.getConsultationFee() 
                : getDefaultConsultationFee();

        // Validate coupon with admin-service
        CouponValidationResponse validationResponse = validateCoupon(
                couponCode,
                request.getBeneficiaryType(),
                request.getBeneficiaryId(),
                request.getPatientId(),
                request.getCaseId(),
                consultationFee);

        if (!validationResponse.getValid()) {
            log.warn("Coupon {} validation failed: {}", couponCode, validationResponse.getMessage());
            throw new BusinessException(
                    "Coupon validation failed: " + validationResponse.getMessage(),
                    HttpStatus.BAD_REQUEST);
        }

        // Calculate amounts
        BigDecimal discountAmount = validationResponse.getDiscountAmount();
        BigDecimal remainingAmount = consultationFee.subtract(discountAmount);
        
        // If there's a remaining amount, additional payment would be needed
        // For now, we only support full coverage or full discount
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            log.info("Coupon provides partial discount. Remaining amount: {}", remainingAmount);
            // Could integrate with Stripe for remaining amount
        }

        // Calculate platform fee (10% of original amount)
        BigDecimal platformFeePercentage = systemConfigService.getPlatformFeePercentage();
        BigDecimal platformFee = consultationFee.multiply(platformFeePercentage);
        BigDecimal doctorAmount = consultationFee.subtract(platformFee);

        // Create payment record
        Payment payment = Payment.builder()
                .patientId(request.getPatientId())
                .doctorId(request.getDoctorId())
                .caseId(request.getCaseId())
                .appointmentId(request.getAppointmentId())
                .paymentType(PaymentType.CONSULTATION)
                .amount(consultationFee)
                .platformFee(platformFee)
                .doctorAmount(doctorAmount)
                .status(PaymentStatus.PENDING)
                .paymentMethod(PaymentMethod.COUPON)
                .currency("USD")
                .idempotencyKey(idempotencyKey)
                .transactionId(generateTransactionId())
                .createdBy(request.getRedeemedByUserId())
                .build();

        // Add metadata
        payment.addMetadata("coupon_code", couponCode);
        payment.addMetadata("coupon_id", String.valueOf(validationResponse.getCouponId()));
        payment.addMetadata("discount_amount", discountAmount.toString());
        payment.addMetadata("discount_type", validationResponse.getDiscountType().name());
        payment.addMetadata("beneficiary_type", request.getBeneficiaryType().name());
        payment.addMetadata("beneficiary_id", String.valueOf(request.getBeneficiaryId()));

        payment = paymentRepository.save(payment);
        log.info("Created payment record {} for coupon {}", payment.getId(), couponCode);

        // Mark coupon as used in admin-service
        MarkCouponUsedResponse usedResponse = markCouponAsUsed(
                couponCode, payment, request, discountAmount, remainingAmount);

        if (!usedResponse.getSuccess()) {
            // Rollback payment status
            payment.setStatus(PaymentStatus.FAILED);
            payment.setGatewayResponse("Failed to mark coupon as used: " + usedResponse.getMessage());
            paymentRepository.save(payment);
            
            throw new BusinessException(
                    "Failed to redeem coupon: " + usedResponse.getMessage(),
                    HttpStatus.BAD_REQUEST);
        }

        // Update payment status to completed
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setProcessedAt(LocalDateTime.now());
        payment.setGatewayResponse("Coupon redeemed successfully");
        payment.addMetadata("admin_coupon_used_at", usedResponse.getUsedAt().toString());
        payment = paymentRepository.save(payment);

        // Send payment completed event
        paymentEventProducer.sendPaymentCompletedEvent(payment);

        log.info("Coupon payment completed successfully. Payment ID: {}, Coupon: {}", 
                payment.getId(), couponCode);

        return CouponPaymentResponseDto.builder()
                .success(true)
                .paymentId(payment.getId())
                .transactionId(payment.getTransactionId())
                .caseId(request.getCaseId())
                .patientId(request.getPatientId())
                .doctorId(request.getDoctorId())
                .couponId(validationResponse.getCouponId())
                .couponCode(couponCode)
                .originalAmount(consultationFee)
                .discountAmount(discountAmount)
                .finalAmount(remainingAmount)
                .platformFee(platformFee)
                .doctorAmount(doctorAmount)
                .currency("USD")
                .status(PaymentStatus.COMPLETED.name())
                .processedAt(payment.getProcessedAt())
                .message("Payment completed using coupon")
                .build();
    }

    /**
     * Mark coupon as used in admin-service.
     */
    private MarkCouponUsedResponse markCouponAsUsed(
            String couponCode,
            Payment payment,
            CouponPaymentRequestDto request,
            BigDecimal discountAmount,
            BigDecimal remainingAmount) {
        
        try {
            MarkCouponUsedRequest usedRequest = MarkCouponUsedRequest.builder()
                    .couponCode(couponCode)
                    .caseId(request.getCaseId())
                    .patientId(request.getPatientId())
                    .paymentId(payment.getId())
                    //.originalAmount(payment.getAmount())
                    .discountApplied(discountAmount)
                    .amountCharged(remainingAmount)
                    .usedAt(LocalDateTime.now())
                    .redeemedByUserId(request.getRedeemedByUserId())
                    .build();

            ResponseEntity<ApiResponse<MarkCouponUsedResponse>> response = 
                    adminCouponClient.markCouponAsUsed(couponCode, usedRequest);

            if (response.getBody() != null && response.getBody().getData() != null) {
                return response.getBody().getData();
            }

            return MarkCouponUsedResponse.builder()
                    .success(false)
                    .message("No response from admin service")
                    .build();

        } catch (Exception e) {
            log.error("Error marking coupon {} as used: {}", couponCode, e.getMessage(), e);
            return MarkCouponUsedResponse.builder()
                    .success(false)
                    .message("Failed to communicate with admin service: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Get coupon details for display.
     */
    @Transactional(readOnly = true)
    public AdminCouponDto getCouponDetails(String couponCode) {
        try {
            ResponseEntity<ApiResponse<AdminCouponDto>> response = 
                    adminCouponClient.getCouponByCode(couponCode);

            if (response.getBody() != null && response.getBody().getData() != null) {
                return response.getBody().getData();
            }
        } catch (Exception e) {
            log.error("Error fetching coupon details for {}: {}", couponCode, e.getMessage());
        }
        return null;
    }

    // ==================== Helper Methods ====================

    private CouponValidationResponse createInvalidResponse(String couponCode, String message, String errorCode) {
        return CouponValidationResponse.builder()
                .valid(false)
                .couponCode(couponCode)
                .message(message)
                .errorCode(errorCode)
                .build();
    }

    private String generateIdempotencyKey(CouponPaymentRequestDto request) {
        return String.format("COUPON_%d_%d_%s_%d",
                request.getCaseId(),
                request.getPatientId(),
                request.getCouponCode(),
                System.currentTimeMillis() / 60000); // 1-minute window
    }

    private String generateTransactionId() {
        return "TXN-COUPON-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private BigDecimal getDefaultConsultationFee() {
        return systemConfigService.getDefaultConsultationFee();
    }
}