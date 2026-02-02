package com.supervisorservice.service;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.CaseDto;
import com.commonlibrary.dto.PaymentDto;
import com.commonlibrary.dto.PaymentHistoryDto;
import com.commonlibrary.dto.ProcessPaymentDto;
import com.commonlibrary.dto.coupon.*;
import com.commonlibrary.entity.*;
import com.commonlibrary.exception.BusinessException;
import com.supervisorservice.dto.AcceptAppointmentDto;
import com.supervisorservice.dto.PayConsultationFeeRequest;
import com.supervisorservice.dto.PaymentResponseDto;
import com.supervisorservice.dto.SupervisorPayConsultationDto;
import com.supervisorservice.entity.MedicalSupervisor;
import com.supervisorservice.entity.SupervisorCouponAllocation;
import com.supervisorservice.feign.AdminServiceClient;
import com.supervisorservice.feign.PatientServiceClient;
import com.supervisorservice.feign.PaymentServiceClient;
import com.supervisorservice.kafka.SupervisorKafkaProducer;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.commonlibrary.entity.CaseStatus.IN_PROGRESS;

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
    private final AdminServiceClient adminCouponClient;
    private final PaymentServiceClient paymentServiceClient;
    private final PatientServiceClient patientServiceClient;
    private final SupervisorKafkaProducer eventProducer;
    private final AppointmentManagementService appointmentManagementService;
    private final PatientManagementService patientManagementService;

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

        //1-  Find local allocation
        log.info("1- Find local allocation");
        SupervisorCouponAllocation allocation = allocationRepository
                .findByCouponCodeAndSupervisorId(couponCode, supervisor.getId())
                .orElseThrow(() -> new BusinessException(
                        "Coupon not found in your allocations", HttpStatus.NOT_FOUND));

        //2-  Validate coupon is assigned to this patient
        log.info("2-  Validate coupon is assigned to this patient wihing supervisor service");
        if (allocation.getAssignedPatientId() == null) {
            throw new BusinessException("Coupon is not assigned to any patient", HttpStatus.BAD_REQUEST);
        }

        if (!allocation.getAssignedPatientId().equals(request.getPatientId())) {
            throw new BusinessException("Coupon is assigned to a different patient", HttpStatus.BAD_REQUEST);
        }

        //3-  Validate coupon status
        log.info("3-  Validate coupon status in supervisor repository");
        if (allocation.getStatus() != SupervisorCouponStatus.ASSIGNED) {
            throw new BusinessException(
                    "Coupon is not available. Status: " + allocation.getStatus(), 
                    HttpStatus.BAD_REQUEST);
        }

        if (allocation.isExpired()) {
            throw new BusinessException("Coupon has expired", HttpStatus.BAD_REQUEST);
        }

//        //4- Validate with admin-service
//        log.info("4- Validate with admin-service");
//        CouponValidationResponse validationResponse = validateWithAdminService(
//                supervisor, couponCode, request.getPatientId(), request.getCaseId(), consultationFee);
//
//        if (!validationResponse.getValid()) {
//            throw new BusinessException(
//                    "Coupon validation failed: " + validationResponse.getMessage(),
//                    HttpStatus.BAD_REQUEST);
//        }

//        //5- Calculate amounts
//        log.info("5- Calculate amounts");
//        BigDecimal discountAmount = validationResponse.getDiscountAmount() != null
//                ? validationResponse.getDiscountAmount()
//                : allocation.calculateDiscount(consultationFee);
//        BigDecimal remainingAmount = consultationFee.subtract(discountAmount);


        //4- Call payment-service to process COUPON payment
        log.info("4- Call payment-service to process COUPON payment");
        SupervisorPayConsultationDto consultationDto = SupervisorPayConsultationDto.builder()
                .appointmentId(request.getAppointmentId())
                .caseId(request.getCaseId())
                .patientId(request.getPatientId())
                .doctorId(request.getDoctorId())
                .paymentMethod(PaymentMethod.COUPON)
                .couponCode(couponCode)
                .amount(request.getAmount())
                .build();
        PaymentDto paymentDto =  notifyPaymentService(consultationDto, supervisor.getId(), PaymentMethod.COUPON);
        if( paymentDto == null ){
            throw new BusinessException(
                    "Failed to call payment service: ", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        //5- Update appointment status to CONFIRMED and Case Status to IN_PROGRESS/Case's payment status to COMPLETED
        log.info("5- Update appointment status and Case Status");
        confirmAppointmentAndCase( consultationDto, supervisor.getId() );


//        //8- Mark coupon as used in admin-service
//        log.info("8- Mark coupon as used in admin-service");
//        MarkCouponUsedResponse usedResponse = markCouponAsUsedInAdminService(
//                couponCode, supervisor, request, discountAmount, remainingAmount);
//
//        if (!usedResponse.getSuccess()) {
//            throw new BusinessException(
//                    "Failed to mark coupon as used: " + usedResponse.getMessage(),
//                    HttpStatus.BAD_REQUEST);
//        }

        //6- Update local allocation
        log.info("6- Update local allocation in supervisor repository");
        allocation.setStatus(SupervisorCouponStatus.USED);
        allocation.setUsedAt(LocalDateTime.now());
        allocation.setUsedForCaseId(request.getCaseId());
        allocation.setUsedForPaymentId(paymentDto.getPaymentId());
        allocation.setLastSyncedAt(LocalDateTime.now());
        allocationRepository.save(allocation);


        log.info("Coupon {} redeemed successfully for case {}", couponCode, request.getCaseId());

        return PaymentResponseDto.builder()
                .paymentId(paymentDto.getPaymentId())
                .caseId(request.getCaseId())
                .patientId(request.getPatientId())
                .supervisorId(supervisor.getId())
                .paymentSource("COUPON")
                .amount(consultationFee)
                //TODO you have to update both discountAmount, and finalAmount
                .discountAmount(paymentDto.getAmount())
                .finalAmount(paymentDto.getAmount())
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
                    //.paymentId()
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

            // Call payment-service to process STRIPE payment
            SupervisorPayConsultationDto consultationDto = SupervisorPayConsultationDto.builder()
                    .appointmentId(request.getAppointmentId())
                    .caseId(request.getCaseId())
                    .patientId(request.getPatientId())
                    .doctorId(request.getDoctorId())
                    .paymentMethod(PaymentMethod.valueOf(request.getPaymentMethod()))
                    .paymentIntentId(Objects.equals(request.getPaymentMethod(),
                            PaymentMethod.STRIPE.toString()) ? request.getStripePaymentIntentId() : "")
                    //.paymentMethodId(request.getPaymentMethod(), PaymentMethod.STRIPE.toString()) ? request.get)
                    .amount(request.getAmount())
                    .build();
            PaymentDto paymentDto =  notifyPaymentService(consultationDto, supervisor.getId(), PaymentMethod.STRIPE);
            if( paymentDto != null ){
                //Update appointment status to be CONFIRMED
                confirmAppointmentAndCase( consultationDto, supervisor.getId() );


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
            }
             else {
                throw new BusinessException("Stripe payment failed", HttpStatus.BAD_REQUEST);
            }
        } catch (BusinessException e) {
            e.printStackTrace();
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
            SupervisorPayConsultationDto consultationDto = SupervisorPayConsultationDto.builder()
                    .appointmentId(request.getAppointmentId())
                    .caseId(request.getCaseId())
                    .patientId(request.getPatientId())
                    .doctorId(request.getDoctorId())
                    .paymentMethod(PaymentMethod.valueOf(request.getPaymentMethod()))
                    .paypalOrderId(Objects.equals(request.getPaymentMethod(),PaymentMethod.PAYPAL.toString()) ?
                                                                            request.getPaypalOrderId() : "")
                    .amount(request.getAmount())
                    .build();
            PaymentDto paymentDto =  notifyPaymentService(consultationDto, supervisor.getId(), PaymentMethod.PAYPAL);

            if (paymentDto != null ) {
                //Update appointment status to be CONFIRMED
                confirmAppointmentAndCase( consultationDto, supervisor.getId() );

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
                patientServiceClient.updateCaseStatus(dto.getCaseId(), IN_PROGRESS.name(), dto.getDoctorId());

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
     * Send payment request to payment service
     */
    private PaymentDto notifyPaymentService(SupervisorPayConsultationDto dto, Long supervisorId,
                                      PaymentMethod paymentMethod) {
        log.info("Supervisor-Service: notifyPaymentService(), supervisorId= {}", supervisorId);

        ProcessPaymentDto paymentDto = null;
        CouponPaymentRequestDto couponPaymentRequestDto = null;
        PaymentDto paymentResponse = new PaymentDto();

        // Call payment service
        // Build payment request for payment-service
        switch (paymentMethod) {
            case COUPON:
            {
                couponPaymentRequestDto = CouponPaymentRequestDto.builder()
                        .couponCode(dto.getCouponCode())
                        .appointmentId(dto.getAppointmentId())
                        .caseId(dto.getCaseId())
                        .patientId(dto.getPatientId())
                        .doctorId(dto.getDoctorId())
                        .supervisorId(supervisorId)
                        .redeemedByUserId(supervisorId)
                        .consultationFee(dto.getAmount())
                        .beneficiaryType(BeneficiaryType.MEDICAL_SUPERVISOR)
                        .beneficiaryId(supervisorId)
                        .build();

                ApiResponse<CouponPaymentResponseDto> paymentDtoResponse;
                try {
                    paymentDtoResponse = paymentServiceClient
                            .processPaymentBySupervisor(couponPaymentRequestDto, supervisorId)
                            .getBody();
                } catch (Exception e) {
                    log.error("{} payment -  Failed to call payment service for case {}: {}",paymentMethod.name(), dto.getCaseId(),
                            e.getMessage());
                    throw new BusinessException("Payment service unavailable: " + e.getMessage(),
                            HttpStatus.SERVICE_UNAVAILABLE);
                }

                if (paymentDtoResponse == null || !paymentDtoResponse.isSuccess()) {
                    String errorMsg = paymentDtoResponse != null ? paymentDtoResponse.getMessage() : "Payment service error";
                    throw new BusinessException(errorMsg, HttpStatus.PAYMENT_REQUIRED);
                }
                CouponPaymentResponseDto couponPaymentResponseDto = paymentDtoResponse.getData();

                if(couponPaymentResponseDto != null){
                    paymentResponse.setPaymentId(couponPaymentResponseDto.getPaymentId());
                    paymentResponse.setPaymentType(PaymentType.CONSULTATION);
                    paymentResponse.setPaymentMethod(paymentMethod);
                    paymentResponse.setAmount(couponPaymentResponseDto.getFinalAmount());
                    paymentResponse.setCurrency("USD");
                    paymentResponse.setCaseId(couponPaymentResponseDto.getCaseId());
                    paymentResponse.setPatientId(couponPaymentResponseDto.getPatientId());
                    paymentResponse.setDoctorId(couponPaymentResponseDto.getDoctorId());
                    paymentResponse.setAppointmentId(dto.getAppointmentId());
                    paymentResponse.setStatus( PaymentStatus.valueOf( couponPaymentResponseDto.getStatus()));
                    paymentResponse.setTransactionId(couponPaymentResponseDto.getTransactionId());

                    log.info("Payment info details: {}", paymentResponse.toString());
                }
                else{
                    throw new BusinessException("Error in payment response", HttpStatus.INTERNAL_SERVER_ERROR);
                }



            }
            case PAYPAL:
            {
                paymentDto = ProcessPaymentDto.builder()
                        .patientId(dto.getPatientId())
                        .doctorId(dto.getDoctorId())
                        .caseId(dto.getCaseId())
                        .appointmentId(dto.getAppointmentId())
                        .supervisorId(supervisorId)
                        .paymentType(PaymentType.CONSULTATION)
                        .amount(dto.getAmount())
                        .paymentMethod("PAYPAL")
                        //.paypalOrderId(dto.getPaypalOrderId())
                        .build();

                ApiResponse<PaymentDto> paymentDtoResponse = null;
                try {
                    paymentDtoResponse = paymentServiceClient
                            .processPaymentSimulation(paymentDto)
                            .getBody();
                } catch (Exception e) {
                    log.error("{} payment -  Failed to call payment service for case {}: {}",paymentMethod.name(), dto.getCaseId(),
                            e.getMessage());
                    throw new BusinessException("Payment service unavailable: " + e.getMessage(),
                            HttpStatus.SERVICE_UNAVAILABLE);
                }

                if (paymentDtoResponse == null || !paymentDtoResponse.isSuccess()) {
                    String errorMsg = paymentDtoResponse != null ? paymentDtoResponse.getMessage() : "Payment service error";
                    throw new BusinessException(errorMsg, HttpStatus.PAYMENT_REQUIRED);
                }
                paymentResponse = paymentDtoResponse.getData();
            }
            case STRIPE:
            {
                paymentDto = ProcessPaymentDto.builder()
                        .patientId(dto.getPatientId())
                        .doctorId(dto.getDoctorId())
                        .caseId(dto.getCaseId())
                        .appointmentId(dto.getAppointmentId())
                        .supervisorId(supervisorId)
                        .paymentType(PaymentType.CONSULTATION)
                        .amount(dto.getAmount())
                        .paymentMethod("STRIPE")
                        //.paymentMethodId(dto.getPaymentMethodId())
                        //.paymentIntentId(dto.getPaymentIntentId())
                        .build();

                ApiResponse<PaymentDto> paymentDtoResponse = null;
                try {
                    paymentDtoResponse = paymentServiceClient
                            .processPaymentSimulation(paymentDto)
                            .getBody();
                } catch (Exception e) {
                    log.error("{} payment -  Failed to call payment service for case {}: {}",paymentMethod.name(), dto.getCaseId(),
                            e.getMessage());
                    throw new BusinessException("Payment service unavailable: " + e.getMessage(),
                            HttpStatus.SERVICE_UNAVAILABLE);
                }

                if (paymentDtoResponse == null || !paymentDtoResponse.isSuccess()) {
                    String errorMsg = paymentDtoResponse != null ? paymentDtoResponse.getMessage() : "Payment service error";
                    throw new BusinessException(errorMsg, HttpStatus.PAYMENT_REQUIRED);
                }
                paymentResponse = paymentDtoResponse.getData();
            }
            break;
        }

        return paymentResponse;
    }

    // ==================== Payment History ====================

    /**
     * Get payment history for supervisor
     * Returns payment history for all assigned patients or specific patient
     * Supports COUPON, STRIPE, PAYPAL payment methods
     *
     * @param userId Supervisor user ID
     * @param patientId Optional patient ID filter (if null, returns for all assigned patients)
     * @return List of payment history records
     */
    public List<PaymentHistoryDto> getPaymentHistory(Long userId, Long patientId) {
        log.info("Fetching payment history for supervisor userId: {}, patientId filter: {}",
                userId, patientId);

        // Get supervisor
        MedicalSupervisor supervisor = getSupervisorByUserId(userId);

        // Get assigned patient IDs
        List<Long> assignedPatientIds;
        if (patientId != null) {
            // Validate that specified patient is assigned to this supervisor
            validatePatientAssignment(supervisor.getId(), patientId);
            assignedPatientIds = List.of(patientId);
            log.info("Fetching payment history for specific patient: {}", patientId);
        } else {
            // Get all assigned patients
            assignedPatientIds = patientManagementService.getAssignedPatientIds(userId);
            log.info("Fetching payment history for {} assigned patients", assignedPatientIds.size());
        }

        // Handle empty case
        if (assignedPatientIds.isEmpty()) {
            log.info("No assigned patients found for supervisor {}", userId);
            return new ArrayList<>();
        }

        // Fetch payment history for each patient and aggregate
        List<PaymentHistoryDto> aggregatedHistory = new ArrayList<>();

        for (Long patientIdIter : assignedPatientIds) {
            try {
                log.debug("Fetching payment history for patient: {}", patientIdIter);

                ApiResponse<List<PaymentHistoryDto>> response =
                        paymentServiceClient.getPatientPaymentHistory(patientIdIter);

                if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                    List<PaymentHistoryDto> patientHistory = response.getData();
                    aggregatedHistory.addAll(patientHistory);
                    log.debug("Added {} payment records for patient {}",
                            patientHistory.size(), patientIdIter);
                } else {
                    log.debug("No payment history found for patient {}", patientIdIter);
                }

            } catch (Exception e) {
                log.error("Failed to fetch payment history for patient {}: {}",
                        patientIdIter, e.getMessage());
                // Continue with other patients - don't fail entire request
            }
        }

        // Sort by processed date (most recent first)
        List<PaymentHistoryDto> sortedHistory = aggregatedHistory.stream()
                .sorted((h1, h2) -> {
                    if (h1.getProcessedAt() == null && h2.getProcessedAt() == null) return 0;
                    if (h1.getProcessedAt() == null) return 1;
                    if (h2.getProcessedAt() == null) return -1;
                    return h2.getProcessedAt().compareTo(h1.getProcessedAt());
                })
                .collect(Collectors.toList());

        log.info("Successfully fetched {} total payment records for supervisor {}",
                sortedHistory.size(), userId);

        return sortedHistory;
    }
}