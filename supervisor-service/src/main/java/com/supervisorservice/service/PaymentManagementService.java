package com.supervisorservice.service;

import com.commonlibrary.dto.*;
import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.coupon.CouponPaymentRequestDto;
import com.commonlibrary.dto.coupon.CouponPaymentResponseDto;
import com.commonlibrary.entity.BeneficiaryType;
import com.commonlibrary.entity.CaseStatus;
import com.commonlibrary.entity.PaymentStatus;
import com.commonlibrary.entity.PaymentType;
import com.commonlibrary.exception.BusinessException;
import com.supervisorservice.dto.*;
import com.supervisorservice.entity.MedicalSupervisor;
import com.supervisorservice.entity.SupervisorPayment;
import com.supervisorservice.entity.SupervisorPayment.PaymentMethodType;
import com.supervisorservice.entity.SupervisorPayment.SupervisorPaymentStatus;
import com.supervisorservice.feign.DoctorServiceClient;
import com.supervisorservice.feign.PatientServiceClient;
import com.supervisorservice.feign.PaymentServiceClient;
import com.supervisorservice.kafka.SupervisorKafkaProducer;
import com.supervisorservice.repository.SupervisorPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.commonlibrary.entity.CaseStatus.IN_PROGRESS;

/**
 * Service for managing payments on behalf of patients
 * Supports Stripe, PayPal, and Coupon payment methods
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentManagementService {

    private final SupervisorValidationService validationService;
    //private final CouponService couponService;
    private final PaymentServiceClient paymentServiceClient;
    private final PatientServiceClient patientServiceClient;
    private final DoctorServiceClient doctorServiceClient;
    private final SupervisorPaymentRepository paymentRepository;
    private final SupervisorKafkaProducer eventProducer;
    private final AppointmentManagementService appointmentManagementService;

    /**
     * Main method: Pay consultation fee on behalf of patient
     * Handles STRIPE, PAYPAL, and COUPON payment methods
     */
    @Transactional
    public SupervisorPaymentResponseDto payConsultationFee(
            Long userId,
            SupervisorPayConsultationDto dto) {

        log.info("Processing consultation payment for case {} by user {}, method: {}",
                dto.getCaseId(), userId, dto.getPaymentMethod());

        // 1. Validate supervisor
        MedicalSupervisor supervisor = validationService.getSupervisorByUserId(userId);
        validationService.validateSupervisorVerified(supervisor);

        // 2. Validate patient assignment
        validationService.validatePatientAssignment(supervisor.getId(), dto.getPatientId());

        // 3. Validate case belongs to patient and is in correct status
        CaseDto caseDto = validateCaseForPayment(dto.getCaseId(), dto.getPatientId());

        // 4. Check for duplicate payment
        if (paymentRepository.existsByCaseIdAndStatus(dto.getCaseId(), SupervisorPaymentStatus.COMPLETED)) {
            throw new BusinessException("Payment already completed for this case", HttpStatus.CONFLICT);
        }

        // 5. Create local payment record
        SupervisorPayment localPayment = createLocalPaymentRecord(supervisor, dto, caseDto);

        // 6. Process payment based on method
        SupervisorPaymentResponseDto response;
        try {
            switch (dto.getPaymentMethod()) {
                case COUPON:
                    response = processCouponPayment(supervisor, dto, caseDto, localPayment);
                    break;
                case STRIPE:
                    response = processStripePayment(supervisor, dto, caseDto, localPayment);
                    break;
                case PAYPAL:
                    response = processPayPalPayment(supervisor, dto, caseDto, localPayment);
                    break;
                default:
                    throw new BusinessException("Unsupported payment method: " + dto.getPaymentMethod(),
                            HttpStatus.BAD_REQUEST);
            }
        } catch (BusinessException e) {
            // Update local payment to failed
            localPayment.setStatus(SupervisorPaymentStatus.FAILED);
            localPayment.setErrorMessage(e.getMessage());
            paymentRepository.save(localPayment);
            throw e;
        } catch (Exception e) {
            // Update local payment to failed
            localPayment.setStatus(SupervisorPaymentStatus.FAILED);
            localPayment.setErrorMessage("Payment processing error: " + e.getMessage());
            paymentRepository.save(localPayment);
            throw new BusinessException("Payment processing failed: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (response.isSuccess()) {
            log.info("Payment completed successfully for case {}, transaction: {}",
                    dto.getCaseId(), response.getTransactionId());
        }

        return response;
    }

    /**
     * Process payment using Coupon (Supervisor exclusive)
     */
    @Transactional
    public SupervisorPaymentResponseDto processCouponPayment(
            MedicalSupervisor supervisor,
            SupervisorPayConsultationDto dto,
            CaseDto caseDto,
            SupervisorPayment localPayment) {

        log.info("Processing coupon payment for case {}", dto.getCaseId());

        if (dto.getCouponCode() == null || dto.getCouponCode().isEmpty()) {
            throw new BusinessException("Coupon code is required for coupon payment", HttpStatus.BAD_REQUEST);
        }

        BigDecimal consultationFee = caseDto.getConsultationFee();
        if (consultationFee == null) {
            throw new BusinessException("Consultation fee not set for case", HttpStatus.BAD_REQUEST);
        }

        // Validate coupon
        CouponValidationResponseDto validation = new CouponValidationResponseDto();
//                couponService.validateCoupon(
//                dto.getCouponCode(),
//                dto.getPatientId(),
//                supervisor.getId(),
//                consultationFee);

        if (!validation.isValid()) {
            throw new BusinessException(validation.getMessage(), HttpStatus.BAD_REQUEST);
        }

        // Check if coupon provides full coverage
        if (validation.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException(
                    String.format("Coupon provides $%.2f discount. Remaining $%.2f requires additional payment method.",
                            validation.getDiscountAmount(), validation.getRemainingAmount()),
                    HttpStatus.PAYMENT_REQUIRED);
        }

        // Redeem coupon
        CouponRedemptionDto redemption = new CouponRedemptionDto();
//                couponService.redeemCoupon(
//                dto.getCouponCode(),
//                dto.getCaseId(),
//                dto.getPatientId(),
//                supervisor.getId(),
//                consultationFee);

        // Update local payment record
        localPayment.setStatus(SupervisorPaymentStatus.COMPLETED);
        localPayment.setDiscountAmount(redemption.getDiscountAmount());
        localPayment.setFinalAmount(redemption.getFinalAmount());
        localPayment.setCouponCode(dto.getCouponCode());
        localPayment.setCouponId(redemption.getCouponId());
        localPayment.setTransactionId(redemption.getTransactionId());
        localPayment.setProcessedAt(LocalDateTime.now());

        paymentRepository.save(localPayment);

        // Call payment service
        // Build payment request for payment-service
        CouponPaymentRequestDto couponPaymentRequestDto = CouponPaymentRequestDto.builder()
                .couponCode(dto.getCouponCode())
                .appointmentId(dto.getAppointmentId())
                .caseId(dto.getCaseId())
                .patientId(dto.getPatientId())
                .doctorId(dto.getDoctorId())
                .consultationFee(consultationFee)
                .beneficiaryType(BeneficiaryType.MEDICAL_SUPERVISOR)
                .beneficiaryId(supervisor.getId())
                .build();

//        ProcessPaymentDto paymentDto = ProcessPaymentDto.builder()
//                .patientId(dto.getPatientId())
//                .doctorId(dto.getDoctorId())
//                .caseId(dto.getCaseId())
//                .paymentType(PaymentType.CONSULTATION)
//                .amount(consultationFee)
//                .paymentMethod("COUPON")
//                //.paymentMethodId(dto.getPaymentMethodId())
//                //.paymentIntentId(dto.getPaymentIntentId())
//                .build();
        ApiResponse<CouponPaymentResponseDto> response;
        try {
            response = paymentServiceClient
                    .processPaymentBySupervisor(couponPaymentRequestDto, supervisor.getId())
                    .getBody();
        } catch (Exception e) {
            log.error("Failed to call payment service for case {}: {}", dto.getCaseId(), e.getMessage());
            throw new BusinessException("Payment service unavailable: " + e.getMessage(),
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        if (response == null || !response.isSuccess()) {
            String errorMsg = response != null ? response.getMessage() : "Payment service error";
            throw new BusinessException(errorMsg, HttpStatus.PAYMENT_REQUIRED);
        }

        //If payment successful, update case and confirm appointment
        try {
            confirmAppointmentAndCase(dto, supervisor.getId());

            log.info("COUPON Payment completed successfully for case {}, transaction: {}",
                    dto.getCaseId(), redemption.getTransactionId());

        } catch (Exception e) {
            log.error("Failed to confirm case/appointment after COUPON payment for case {}: {}",
                    dto.getCaseId(), e.getMessage());
            // Payment succeeded but confirmation failed - needs manual resolution
            response.setMessage(response.getMessage() +
                    " Warning: Case confirmation pending, please contact support.");
        }

        return SupervisorPaymentResponseDto.builder()
                .success(true)
                .paymentId(localPayment.getId())
                .caseId(dto.getCaseId())
                .patientId(dto.getPatientId())
                .doctorId(dto.getDoctorId())
                .supervisorId(supervisor.getId())
                .paymentMethod("COUPON")
                .amount(consultationFee)
                .discountAmount(redemption.getDiscountAmount())
                .finalAmount(redemption.getFinalAmount())
                .couponCode(dto.getCouponCode())
                .transactionId(redemption.getTransactionId())
                .processedAt(LocalDateTime.now())
                .message("Payment successful using coupon")
                .build();
    }

    /**
     * Process payment using Stripe
     */
    private SupervisorPaymentResponseDto processStripePayment(
            MedicalSupervisor supervisor,
            SupervisorPayConsultationDto dto,
            CaseDto caseDto,
            SupervisorPayment localPayment) {

        log.info("Processing Stripe payment for case {}", dto.getCaseId());

        BigDecimal consultationFee = caseDto.getConsultationFee();
        if (consultationFee == null) {
            throw new BusinessException("Consultation fee not set for case", HttpStatus.BAD_REQUEST);
        }

        // Build payment request for payment-service
        ProcessPaymentDto paymentDto = ProcessPaymentDto.builder()
                .patientId(dto.getPatientId())
                .doctorId(dto.getDoctorId())
                .caseId(dto.getCaseId())
                .appointmentId(dto.getAppointmentId())
                .supervisorId(supervisor.getId())
                .paymentType(PaymentType.CONSULTATION)
                .amount(consultationFee)
                .paymentMethod("STRIPE")
                //.paymentMethodId(dto.getPaymentMethodId())
                //.paymentIntentId(dto.getPaymentIntentId())
                .build();

        // Call payment service
        ApiResponse<PaymentDto> response;
        try {
            response = paymentServiceClient
                    .processPaymentSimulation(paymentDto)
                    .getBody();
        } catch (Exception e) {
            log.error("Failed to call payment service for case {}: {}", dto.getCaseId(), e.getMessage());
            throw new BusinessException("Payment service unavailable: " + e.getMessage(),
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        if (response == null || !response.isSuccess()) {
            String errorMsg = response != null ? response.getMessage() : "Payment service error";
            throw new BusinessException(errorMsg, HttpStatus.PAYMENT_REQUIRED);
        }

        PaymentDto payment = response.getData();

        //TODO must handle Stripe values
        // Update local payment record
        localPayment.setStatus(SupervisorPaymentStatus.COMPLETED);
        localPayment.setFinalAmount(consultationFee);
        //localPayment.setExternalPaymentId(payment.getId());
        //localPayment.setStripePaymentIntentId(payment.getStripePaymentIntentId());
        //localPayment.setStripeChargeId(payment.getStripeChargeId());
        localPayment.setTransactionId(payment.getTransactionId());
        //localPayment.setReceiptUrl(payment.getReceiptUrl());
        localPayment.setProcessedAt(LocalDateTime.now());
        paymentRepository.save(localPayment);

        //If payment successful, update case and confirm appointment
        try {
            confirmAppointmentAndCase(dto, supervisor.getId());

            log.info("Stripe Payment completed successfully for case {}, transaction: {}",
                    dto.getCaseId(), payment.getTransactionId());

        } catch (Exception e) {
            log.error("Failed to confirm case/appointment after Stripe payment for case {}: {}",
                    dto.getCaseId(), e.getMessage());
            // Payment succeeded but confirmation failed - needs manual resolution
            response.setMessage(response.getMessage() +
                    " Warning: Case confirmation pending, please contact support.");
        }


        return SupervisorPaymentResponseDto.builder()
                .success(true)
                .paymentId(localPayment.getId())
                .caseId(dto.getCaseId())
                .patientId(dto.getPatientId())
                .doctorId(dto.getDoctorId())
                .supervisorId(supervisor.getId())
                .paymentMethod("STRIPE")
                .amount(consultationFee)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(consultationFee)
                //.stripePaymentIntentId(payment.getStripePaymentIntentId())
                //.stripeChargeId(payment.getStripeChargeId())
                .transactionId(payment.getTransactionId())
                //.receiptUrl(payment.getReceiptUrl())
                .processedAt(LocalDateTime.now())
                .message("Payment successful via Stripe")
                .build();
    }

    /**
     * Process payment using PayPal
     */
    private SupervisorPaymentResponseDto processPayPalPayment(
            MedicalSupervisor supervisor,
            SupervisorPayConsultationDto dto,
            CaseDto caseDto,
            SupervisorPayment localPayment) {

        log.info("Processing PayPal payment for case {}", dto.getCaseId());

        BigDecimal consultationFee = caseDto.getConsultationFee();
        if (consultationFee == null) {
            throw new BusinessException("Consultation fee not set for case", HttpStatus.BAD_REQUEST);
        }

        if (dto.getPaypalOrderId() == null || dto.getPaypalOrderId().isEmpty()) {
            throw new BusinessException("PayPal order ID is required", HttpStatus.BAD_REQUEST);
        }

        // Build payment request for payment-service
        ProcessPaymentDto paymentDto = ProcessPaymentDto.builder()
                .patientId(dto.getPatientId())
                .doctorId(dto.getDoctorId())
                .caseId(dto.getCaseId())
                .appointmentId(dto.getAppointmentId())
                .supervisorId(supervisor.getId())
                .paymentType(PaymentType.CONSULTATION)
                .amount(consultationFee)
                .paymentMethod("PAYPAL")
                //.paypalOrderId(dto.getPaypalOrderId())
                .build();

        // Call payment service
        ApiResponse<PaymentDto> response;
        try {
            response = paymentServiceClient
                    .processPaymentSimulation(paymentDto)
                    .getBody();
        } catch (Exception e) {
            log.error("Failed to call payment service for case {}: {}", dto.getCaseId(), e.getMessage());
            throw new BusinessException("Payment service unavailable: " + e.getMessage(),
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        if (response == null || !response.isSuccess()) {
            String errorMsg = response != null ? response.getMessage() : "Payment service error";
            throw new BusinessException(errorMsg, HttpStatus.PAYMENT_REQUIRED);
        }

        PaymentDto payment = response.getData();

        // Update local payment record
        localPayment.setStatus(SupervisorPaymentStatus.COMPLETED);
        localPayment.setFinalAmount(consultationFee);
        //localPayment.setExternalPaymentId(payment.getId());
        localPayment.setPaypalOrderId(dto.getPaypalOrderId());
        localPayment.setTransactionId(payment.getTransactionId());
        //localPayment.setReceiptUrl(payment.getReceiptUrl());
        localPayment.setProcessedAt(LocalDateTime.now());
        paymentRepository.save(localPayment);

        //If payment successful, update case and confirm appointment
        try {
            confirmAppointmentAndCase(dto, supervisor.getId());

            log.info("PayPal Payment completed successfully for case {}, transaction: {}",
                    dto.getCaseId(), payment.getTransactionId());

        } catch (Exception e) {
            log.error("Failed to confirm case/appointment after PayPal payment for case {}: {}",
                    dto.getCaseId(), e.getMessage());
            // Payment succeeded but confirmation failed - needs manual resolution
            response.setMessage(response.getMessage() +
                    " Warning: Case confirmation pending, please contact support.");
        }

        return SupervisorPaymentResponseDto.builder()
                .success(true)
                .paymentId(localPayment.getId())
                .caseId(dto.getCaseId())
                .patientId(dto.getPatientId())
                .doctorId(dto.getDoctorId())
                .supervisorId(supervisor.getId())
                .paymentMethod("PAYPAL")
                .amount(consultationFee)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(consultationFee)
                .paypalOrderId(dto.getPaypalOrderId())
                .transactionId(payment.getTransactionId())
                //.receiptUrl(payment.getReceiptUrl())
                .processedAt(LocalDateTime.now())
                .message("Payment successful via PayPal")
                .build();
    }

    /**
     * Create Stripe payment intent for consultation
     */
    public PaymentIntentDto createPaymentIntent(
            Long userId,
            Long caseId,
            Long patientId,
            Long doctorId) {

        log.info("Creating payment intent for case {} by user {}", caseId, userId);

        MedicalSupervisor supervisor = validationService.getSupervisorByUserId(userId);
        validationService.validateSupervisorVerified(supervisor);
        validationService.validatePatientAssignment(supervisor.getId(), patientId);

        CaseDto caseDto = validateCaseForPayment(caseId, patientId);

        try {
            ApiResponse<Map<String, Object>> response = paymentServiceClient
                    .createConsultationPaymentIntent(caseId, patientId, doctorId, supervisor.getId())
                    .getBody();

            if (response == null || !response.isSuccess()) {
                throw new BusinessException("Failed to create payment intent", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            Map<String, Object> intentData = response.getData();

            return PaymentIntentDto.builder()
                    .paymentId(intentData.get("paymentId") != null ? 
                            Long.valueOf(intentData.get("paymentId").toString()) : null)
                    .paymentIntentId((String) intentData.get("paymentIntentId"))
                    .clientSecret((String) intentData.get("clientSecret"))
                    .amount(caseDto.getConsultationFee())
                    .currency("USD")
                    .status((String) intentData.get("status"))
                    .caseId(caseId)
                    .patientId(patientId)
                    .doctorId(doctorId)
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create payment intent for case {}: {}", caseId, e.getMessage());
            throw new BusinessException("Payment service error: " + e.getMessage(),
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Validate coupon before payment
     */
    public CouponValidationResponseDto validateCouponForPayment(
            Long userId,
            String couponCode,
            Long patientId,
            Long caseId) {

        log.info("Validating coupon {} for case {} by user {}", couponCode, caseId, userId);

        MedicalSupervisor supervisor = validationService.getSupervisorByUserId(userId);
        validationService.validatePatientAssignment(supervisor.getId(), patientId);

        CaseDto caseDto = getCaseDetails(caseId, patientId);

        if (caseDto.getConsultationFee() == null) {
            throw new BusinessException("Consultation fee not set for case", HttpStatus.BAD_REQUEST);
        }

        return null;
//        return couponService.validateCoupon(
//                couponCode,
//                patientId,
//                supervisor.getId(),
//                caseDto.getConsultationFee());
    }

    /**
     * Get payment history for supervisor
     */
    public List<SupervisorPaymentResponseDto> getPaymentHistory(Long userId, Long patientId) {
        MedicalSupervisor supervisor = validationService.getSupervisorByUserId(userId);

        List<SupervisorPayment> payments;
        if (patientId != null) {
            validationService.validatePatientAssignment(supervisor.getId(), patientId);
            payments = paymentRepository.findBySupervisorIdAndPatientIdOrderByCreatedAtDesc(
                    supervisor.getId(), patientId);
        } else {
            payments = paymentRepository.findBySupervisorIdOrderByCreatedAtDesc(supervisor.getId());
        }

        return payments.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Validate case is ready for payment
     */
    private CaseDto validateCaseForPayment(Long caseId, Long patientId) {
        CaseDto caseDto = getCaseDetails(caseId, patientId);

        // Check case status - should be SCHEDULED or PAYMENT_PENDING
        if (caseDto.getStatus() != CaseStatus.SCHEDULED && 
                caseDto.getStatus() != CaseStatus.PAYMENT_PENDING) {
            throw new BusinessException(
                    "Case is not in a valid status for payment. Current status: " + caseDto.getStatus(),
                    HttpStatus.BAD_REQUEST);
        }

        // Check if already paid
        if (caseDto.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new BusinessException("Case has already been paid", HttpStatus.CONFLICT);
        }

        // Check consultation fee is set
        if (caseDto.getConsultationFee() == null || 
                caseDto.getConsultationFee().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Consultation fee not set for case", HttpStatus.BAD_REQUEST);
        }

        return caseDto;
    }

    /**
     * Get case details from patient service
     */
    private CaseDto getCaseDetails(Long caseId, Long patientId) {
        try {
            ApiResponse<CaseDto> response = patientServiceClient.getCaseById(caseId).getBody();

            if (response == null || response.getData() == null) {
                throw new BusinessException("Case not found", HttpStatus.NOT_FOUND);
            }

            CaseDto caseDto = response.getData();

            // Validate case belongs to patient
            if (!caseDto.getPatientId().equals(patientId)) {
                throw new BusinessException("Case does not belong to the specified patient",
                        HttpStatus.FORBIDDEN);
            }

            return caseDto;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get case details for case {}: {}", caseId, e.getMessage());
            throw new BusinessException("Failed to retrieve case details: " + e.getMessage(),
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Confirm appointment and update case after successful payment
     */
    private void confirmAppointmentAndCase(SupervisorPayConsultationDto dto, Long supervisorId) {
        log.info("Confirming appointment and case for case {} by supervisor {}", 
                dto.getCaseId(), supervisorId);
        try{

            try{
                //Accept Appointment:
                AcceptAppointmentDto acceptAppointmentDto = new AcceptAppointmentDto();
                acceptAppointmentDto.setCaseId(dto.getCaseId());
                acceptAppointmentDto.setPatientId(dto.getPatientId());
                acceptAppointmentDto.setNotes(dto.getNotes());
                appointmentManagementService.acceptAppointment(supervisorId, acceptAppointmentDto);

            } catch (Exception e) {
                log.error("Failed to accept appointment {} for case status for case {}: {}",
                        dto.getAppointmentId() ,dto.getCaseId(), e.getMessage());
                throw new BusinessException("Failed to accept appointment for the case " + e.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }

            try{
                //Update Case's status and PaymentStatus
                patientServiceClient.updateCaseStatus(dto.getCaseId(),
                        IN_PROGRESS.name(), dto.getDoctorId()).getBody().isSuccess();

            } catch (Exception e) {
                log.error("Failed to update case {} status, for appointment {}: {}",
                       dto.getCaseId() ,dto.getAppointmentId(), e.getMessage());
                throw new BusinessException("Failed to update case status after confirming the appointment " +
                        e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            //Send Kafka Event
            eventProducer.sendScheduleConfirmationEvent( dto.getCaseId(), dto.getPatientId(), dto.getDoctorId());

        } catch (Exception e) {
                e.printStackTrace();

        }

//        // Update case payment status via patient-service
//        try {
//            patientServiceClient.acceptAppointmentBySupervisor(
//                    dto.getCaseId(),
//                    dto.getPatientId(),
//                    supervisorId);
//        } catch (Exception e) {
//            log.error("Failed to update case status for case {}: {}", dto.getCaseId(), e.getMessage());
//            throw new BusinessException("Failed to confirm case: " + e.getMessage(),
//                    HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//
//        // Confirm appointment with doctor-service if appointment ID provided
//        if (dto.getAppointmentId() != null) {
//            try {
//                doctorServiceClient.confirmAppointment(
//                        dto.getCaseId(),
//                        dto.getPatientId(),
//                        dto.getDoctorId());
//            } catch (Exception e) {
//                log.warn("Failed to confirm appointment {} with doctor service: {}",
//                        dto.getAppointmentId(), e.getMessage());
//                // Don't fail the payment - appointment confirmation can be retried
//            }
//        }
    }

    /**
     * Create local payment record
     */
    private SupervisorPayment createLocalPaymentRecord(
            MedicalSupervisor supervisor,
            SupervisorPayConsultationDto dto,
            CaseDto caseDto) {

        PaymentMethodType methodType;
        switch (dto.getPaymentMethod()) {
            case STRIPE:
                methodType = PaymentMethodType.STRIPE;
                break;
            case PAYPAL:
                methodType = PaymentMethodType.PAYPAL;
                break;
            case COUPON:
                methodType = PaymentMethodType.COUPON;
                break;
            default:
                methodType = PaymentMethodType.STRIPE;
        }

        SupervisorPayment payment = SupervisorPayment.builder()
                .supervisorId(supervisor.getId())
                .patientId(dto.getPatientId())
                .caseId(dto.getCaseId())
                .doctorId(dto.getDoctorId())
                .appointmentId(dto.getAppointmentId())
                .paymentMethod(methodType)
                .originalAmount(caseDto.getConsultationFee())
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(caseDto.getConsultationFee())
                .status(SupervisorPaymentStatus.PENDING)
                .notes(dto.getNotes())
                .build();

        return paymentRepository.save(payment);
    }

    /**
     * Map entity to response DTO
     */
    private SupervisorPaymentResponseDto mapToResponseDto(SupervisorPayment payment) {
        return SupervisorPaymentResponseDto.builder()
                .success(payment.getStatus() == SupervisorPaymentStatus.COMPLETED)
                .paymentId(payment.getId())
                .caseId(payment.getCaseId())
                .patientId(payment.getPatientId())
                .doctorId(payment.getDoctorId())
                .supervisorId(payment.getSupervisorId())
                .paymentMethod(payment.getPaymentMethod().name())
                .amount(payment.getOriginalAmount())
                .discountAmount(payment.getDiscountAmount())
                .finalAmount(payment.getFinalAmount())
                .couponCode(payment.getCouponCode())
                .stripePaymentIntentId(payment.getStripePaymentIntentId())
                .stripeChargeId(payment.getStripeChargeId())
                .paypalOrderId(payment.getPaypalOrderId())
                .transactionId(payment.getTransactionId())
                .receiptUrl(payment.getReceiptUrl())
                .processedAt(payment.getProcessedAt())
                .errorDetails(payment.getErrorMessage())
                .build();
    }
}