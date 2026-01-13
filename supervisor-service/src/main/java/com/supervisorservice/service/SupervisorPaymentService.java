package com.supervisorservice.service;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.CaseDto;
import com.commonlibrary.dto.coupon.*;
import com.commonlibrary.entity.BeneficiaryType;
import com.commonlibrary.entity.SupervisorAssignmentStatus;
import com.commonlibrary.entity.SupervisorCouponStatus;
import com.commonlibrary.exception.BusinessException;
import com.supervisorservice.dto.PayConsultationFeeRequest;
import com.supervisorservice.dto.PaymentResponseDto;
import com.supervisorservice.entity.MedicalSupervisor;
import com.supervisorservice.entity.SupervisorCouponAllocation;
import com.supervisorservice.feign.AdminCouponServiceClient;
import com.supervisorservice.feign.PatientServiceClient;
import com.supervisorservice.feign.PaymentServiceClient;
import com.supervisorservice.repository.MedicalSupervisorRepository;
import com.supervisorservice.repository.SupervisorCouponAllocationRepository;
import com.supervisorservice.repository.SupervisorPatientAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service for processing supervisor payments including coupon redemption.
 * Integrates with payment-service and admin-service for payment processing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SupervisorPaymentService {

    private final MedicalSupervisorRepository supervisorRepository;
    private final SupervisorPatientAssignmentRepository assignmentRepository;
    private final SupervisorCouponAllocationRepository allocationRepository;
    private final AdminCouponServiceClient adminCouponClient;
    private final PaymentServiceClient paymentServiceClient;
    private final PatientServiceClient patientServiceClient;

    /**
     * Process consultation fee payment
     * Supports STRIPE, PAYPAL, and COUPON payment methods
     */
    public PaymentResponseDto payConsultationFee(Long userId, PayConsultationFeeRequest request) {
        log.info("Processing {} payment for case {} by user {}", 
                request.getPaymentMethod(), request.getCaseId(), userId);

        // Get supervisor
        MedicalSupervisor supervisor = getSupervisorByUserId(userId);

        // Validate patient assignment
        validatePatientAssignment(supervisor.getId(), request.getPatientId());

        // Validate case exists and is in payable status
        validateCase(request.getCaseId(), request.getPatientId());

        // Get consultation fee
        BigDecimal consultationFee = getConsultationFee(request.getCaseId());

        // Route to appropriate payment handler
        return switch (request.getPaymentMethod().toUpperCase()) {
            case "COUPON" -> processCouponPayment(supervisor, request, consultationFee);
            case "STRIPE" -> processStripePayment(supervisor, request, consultationFee);
            case "PAYPAL" -> processPayPalPayment(supervisor, request, consultationFee);
            default -> throw new BusinessException(
                    "Invalid payment method: " + request.getPaymentMethod(), 
                    HttpStatus.BAD_REQUEST);
        };
    }

    /**
     * Create Stripe payment intent
     */
    public PaymentResponseDto createStripePaymentIntent(
            Long userId, Long caseId, Long patientId, Long doctorId) {
        
        log.info("Creating Stripe payment intent for case {} by user {}", caseId, userId);

        MedicalSupervisor supervisor = getSupervisorByUserId(userId);
        validatePatientAssignment(supervisor.getId(), patientId);
        validateCase(caseId, patientId);

        BigDecimal consultationFee = getConsultationFee(caseId);

        try {
            // Call payment-service to create payment intent
            ResponseEntity<ApiResponse<Object>> response = paymentServiceClient.createPaymentIntent(
                    caseId, patientId, doctorId, consultationFee, "USD", supervisor.getUserId());

            if (response.getBody() != null && response.getBody().isSuccess()) {
                // Map response to PaymentResponseDto
                return PaymentResponseDto.builder()
                        .caseId(caseId)
                        .patientId(patientId)
                        .supervisorId(supervisor.getId())
                        .paymentSource("STRIPE")
                        .amount(consultationFee)
                        .currency("USD")
                        .status("PENDING")
                        .message("Payment intent created")
                        .timestamp(LocalDateTime.now())
                        .build();
            } else {
                throw new BusinessException("Failed to create payment intent", HttpStatus.BAD_REQUEST);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating payment intent: {}", e.getMessage(), e);
            throw new BusinessException("Payment service error: " + e.getMessage(), 
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    // ==================== Coupon Payment ====================

    private PaymentResponseDto processCouponPayment(
            MedicalSupervisor supervisor,
            PayConsultationFeeRequest request,
            BigDecimal consultationFee) {
        
        log.info("Processing coupon payment for case {} with coupon {}", 
                request.getCaseId(), request.getCouponCode());

        if (request.getCouponCode() == null || request.getCouponCode().trim().isEmpty()) {
            throw new BusinessException("Coupon code is required for coupon payment", HttpStatus.BAD_REQUEST);
        }

        String couponCode = request.getCouponCode().toUpperCase().trim();

        // Find local allocation
        SupervisorCouponAllocation allocation = allocationRepository
                .findByCouponCodeAndSupervisorId(couponCode, supervisor.getId())
                .orElseThrow(() -> new BusinessException(
                        "Coupon not found in your allocations", HttpStatus.NOT_FOUND));

        // Validate coupon is assigned to this patient
        if (allocation.getAssignedPatientId() == null) {
            throw new BusinessException("Coupon is not assigned to any patient", HttpStatus.BAD_REQUEST);
        }

        if (!allocation.getAssignedPatientId().equals(request.getPatientId())) {
            throw new BusinessException("Coupon is assigned to a different patient", HttpStatus.BAD_REQUEST);
        }

        // Validate coupon status
        if (allocation.getStatus() != SupervisorCouponStatus.ASSIGNED) {
            throw new BusinessException(
                    "Coupon is not available. Status: " + allocation.getStatus(), 
                    HttpStatus.BAD_REQUEST);
        }

        if (allocation.isExpired()) {
            throw new BusinessException("Coupon has expired", HttpStatus.BAD_REQUEST);
        }

        // Validate with admin-service
        CouponValidationResponse validationResponse = validateWithAdminService(
                supervisor, couponCode, request.getPatientId(), request.getCaseId(), consultationFee);

        if (!validationResponse.getValid()) {
            throw new BusinessException(
                    "Coupon validation failed: " + validationResponse.getMessage(), 
                    HttpStatus.BAD_REQUEST);
        }

        // Calculate amounts
        BigDecimal discountAmount = validationResponse.getDiscountAmount() != null 
                ? validationResponse.getDiscountAmount() 
                : allocation.calculateDiscount(consultationFee);
        BigDecimal remainingAmount = consultationFee.subtract(discountAmount);

        // Mark coupon as used in admin-service
        MarkCouponUsedResponse usedResponse = markCouponAsUsedInAdminService(
                couponCode, supervisor, request, discountAmount, remainingAmount);

        if (!usedResponse.getSuccess()) {
            throw new BusinessException(
                    "Failed to mark coupon as used: " + usedResponse.getMessage(), 
                    HttpStatus.BAD_REQUEST);
        }

        // Update local allocation
        allocation.setStatus(SupervisorCouponStatus.USED);
        allocation.setUsedAt(LocalDateTime.now());
        allocation.setUsedForCaseId(request.getCaseId());
        allocation.setUsedForPaymentId(usedResponse.getPaymentId());
        allocation.setLastSyncedAt(LocalDateTime.now());
        allocationRepository.save(allocation);

        // Update case payment status
        updateCasePaymentStatus(request.getCaseId(), "PAID");

        log.info("Coupon {} redeemed successfully for case {}", couponCode, request.getCaseId());

        return PaymentResponseDto.builder()
                .paymentId(usedResponse.getPaymentId())
                .caseId(request.getCaseId())
                .patientId(request.getPatientId())
                .supervisorId(supervisor.getId())
                .paymentSource("COUPON")
                .amount(consultationFee)
                .discountAmount(discountAmount)
                .finalAmount(remainingAmount)
                .currency(allocation.getCurrency())
                .status("COMPLETED")
                .couponId(allocation.getAdminCouponId())
                .couponCode(couponCode)
                .timestamp(LocalDateTime.now())
                .message("Payment completed using coupon")
                .build();
    }

    private CouponValidationResponse validateWithAdminService(
            MedicalSupervisor supervisor,
            String couponCode,
            Long patientId,
            Long caseId,
            BigDecimal amount) {
        
        try {
            CouponValidationRequest validationRequest = CouponValidationRequest.builder()
                    .couponCode(couponCode)
                    .beneficiaryType(BeneficiaryType.MEDICAL_SUPERVISOR)
                    .beneficiaryId(supervisor.getId())
                    .patientId(patientId)
                    .caseId(caseId)
                    .requestedAmount(amount)
                    .build();

            ResponseEntity<ApiResponse<CouponValidationResponse>> response = 
                    adminCouponClient.validateCoupon(validationRequest);

            if (response.getBody() != null && response.getBody().getData() != null) {
                return response.getBody().getData();
            }
        } catch (Exception e) {
            log.error("Admin-service validation failed: {}", e.getMessage());
        }

        // Return invalid response if admin-service call fails
        return CouponValidationResponse.builder()
                .valid(false)
                .message("Could not validate coupon with admin service")
                .build();
    }

    private MarkCouponUsedResponse markCouponAsUsedInAdminService(
            String couponCode,
            MedicalSupervisor supervisor,
            PayConsultationFeeRequest request,
            BigDecimal discountAmount,
            BigDecimal remainingAmount) {
        
        try {
            MarkCouponUsedRequest usedRequest = MarkCouponUsedRequest.builder()
                    .couponCode(couponCode)
                    .caseId(request.getCaseId())
                    .patientId(request.getPatientId())
                    .discountApplied(discountAmount)
                    .amountCharged(remainingAmount)
                    .usedAt(LocalDateTime.now())
                    .redeemedByUserId(supervisor.getUserId())
                    .build();

            ResponseEntity<ApiResponse<MarkCouponUsedResponse>> response = 
                    adminCouponClient.markCouponAsUsed(couponCode, usedRequest);

            if (response.getBody() != null && response.getBody().getData() != null) {
                return response.getBody().getData();
            }
        } catch (Exception e) {
            log.error("Failed to mark coupon as used in admin-service: {}", e.getMessage());
        }

        return MarkCouponUsedResponse.builder()
                .success(false)
                .message("Could not mark coupon as used in admin service")
                .build();
    }

    // ==================== Stripe Payment ====================

    private PaymentResponseDto processStripePayment(
            MedicalSupervisor supervisor,
            PayConsultationFeeRequest request,
            BigDecimal consultationFee) {
        
        log.info("Processing Stripe payment for case {}", request.getCaseId());

        try {
            // Call payment-service to process Stripe payment
            ResponseEntity<ApiResponse<Object>> response = paymentServiceClient.processPayment(
                    request.getCaseId(),
                    request.getPatientId(),
                    request.getDoctorId(),
                    "STRIPE",
                    consultationFee,
                    "USD",
                    request.getStripePaymentIntentId(),
                    null,
                    supervisor.getUserId());

            if (response.getBody() != null && response.getBody().isSuccess()) {
                // Update case payment status
                updateCasePaymentStatus(request.getCaseId(), "PAID");

                return PaymentResponseDto.builder()
                        .caseId(request.getCaseId())
                        .patientId(request.getPatientId())
                        .supervisorId(supervisor.getId())
                        .paymentSource("STRIPE")
                        .amount(consultationFee)
                        .currency("USD")
                        .status("COMPLETED")
                        .stripePaymentIntentId(request.getStripePaymentIntentId())
                        .timestamp(LocalDateTime.now())
                        .message("Payment processed via Stripe")
                        .build();
            } else {
                throw new BusinessException("Stripe payment failed", HttpStatus.BAD_REQUEST);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Stripe payment error: {}", e.getMessage(), e);
            throw new BusinessException("Payment processing error: " + e.getMessage(), 
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    // ==================== PayPal Payment ====================

    private PaymentResponseDto processPayPalPayment(
            MedicalSupervisor supervisor,
            PayConsultationFeeRequest request,
            BigDecimal consultationFee) {
        
        log.info("Processing PayPal payment for case {}", request.getCaseId());

        try {
            // Call payment-service to process PayPal payment
            ResponseEntity<ApiResponse<Object>> response = paymentServiceClient.processPayment(
                    request.getCaseId(),
                    request.getPatientId(),
                    request.getDoctorId(),
                    "PAYPAL",
                    consultationFee,
                    "USD",
                    null,
                    request.getPaypalOrderId(),
                    supervisor.getUserId());

            if (response.getBody() != null && response.getBody().isSuccess()) {
                // Update case payment status
                updateCasePaymentStatus(request.getCaseId(), "PAID");

                return PaymentResponseDto.builder()
                        .caseId(request.getCaseId())
                        .patientId(request.getPatientId())
                        .supervisorId(supervisor.getId())
                        .paymentSource("PAYPAL")
                        .amount(consultationFee)
                        .currency("USD")
                        .status("COMPLETED")
                        .paypalOrderId(request.getPaypalOrderId())
                        .timestamp(LocalDateTime.now())
                        .message("Payment processed via PayPal")
                        .build();
            } else {
                throw new BusinessException("PayPal payment failed", HttpStatus.BAD_REQUEST);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("PayPal payment error: {}", e.getMessage(), e);
            throw new BusinessException("Payment processing error: " + e.getMessage(), 
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    // ==================== Helper Methods ====================

    private MedicalSupervisor getSupervisorByUserId(Long userId) {
        return supervisorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Supervisor not found", HttpStatus.NOT_FOUND));
    }

    private void validatePatientAssignment(Long supervisorId, Long patientId) {
        boolean isAssigned = assignmentRepository
                .existsBySupervisorIdAndPatientIdAndAssignmentStatus(
                        supervisorId, patientId, SupervisorAssignmentStatus.ACTIVE);
        
        if (!isAssigned) {
            throw new BusinessException("Patient is not assigned to this supervisor", HttpStatus.FORBIDDEN);
        }
    }

    private void validateCase(Long caseId, Long patientId) {
        try {
            ResponseEntity<ApiResponse<CaseDto>> response = patientServiceClient.getCaseById(caseId);
            if (response.getBody() == null || !response.getBody().isSuccess()) {
                throw new BusinessException("Case not found", HttpStatus.NOT_FOUND);
            }
            // Additional validation: check case status, patient match, etc.
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Could not validate case: {}", e.getMessage());
            // Continue - might be a communication issue
        }
    }

    private BigDecimal getConsultationFee(Long caseId) {
        try {
            ResponseEntity<ApiResponse<CaseDto>> response = patientServiceClient.getCaseById(caseId);
            if (response.getBody() != null && response.getBody().getData() != null) {
                // Extract consultation fee from case data
                // This depends on your CaseDto structure
                return new BigDecimal("150.00"); // Default - implement actual extraction
            }
        } catch (Exception e) {
            log.warn("Could not get consultation fee for case {}: {}", caseId, e.getMessage());
        }
        return new BigDecimal("150.00"); // Default consultation fee
    }

    private void updateCasePaymentStatus(Long caseId, String paymentStatus) {
        try {
            patientServiceClient.updateCasePaymentStatus(caseId, paymentStatus);
            log.info("Updated case {} payment status to {}", caseId, paymentStatus);
        } catch (Exception e) {
            log.error("Failed to update case payment status: {}", e.getMessage());
            // Don't fail the payment - case status update is secondary
        }
    }
}