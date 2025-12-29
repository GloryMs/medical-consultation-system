package com.paymentservice.service;

import com.commonlibrary.entity.PaymentMethod;
import com.commonlibrary.entity.PaymentStatus;
import com.commonlibrary.entity.PaymentType;
import com.paymentservice.dto.PaymentIntentDto;
import com.paymentservice.dto.ProcessConsultationPaymentDto;
import com.paymentservice.entity.Payment;
import com.paymentservice.exception.StripePaymentException;
import com.paymentservice.kafka.PaymentEventProducer;
import com.paymentservice.repository.PaymentRepository;
import com.paymentservice.service.stripe.StripeConsultationFeeService;
import com.paymentservice.service.stripe.StripePaymentGateway;
import com.stripe.model.BalanceTransaction;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for processing consultation payments
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProcessingService {
    
    private final StripePaymentGateway stripePaymentGateway;
    private final PaymentRepository paymentRepository;
    private final StripeConsultationFeeService stripeConsultationFeeService;
    private final SystemConfigurationService systemConfigService;
    private final PaymentEventProducer paymentEventProducer;
    
    /**
     * Initiate consultation payment
     */
    @Transactional
    public PaymentIntentDto initiateConsultationPayment(ProcessConsultationPaymentDto dto, 
                                                        String doctorSpecialization) 
            throws StripePaymentException {
        
        // Check for duplicate payment using idempotency key
        String idempotencyKey = generateIdempotencyKey(dto);
        paymentRepository.findByIdempotencyKey(idempotencyKey)
            .ifPresent(payment -> {
                throw new RuntimeException("Duplicate payment request");
            });
        
        // Get consultation fee based on doctor's specialization
        BigDecimal consultationFee = stripeConsultationFeeService.getApplicableFee(doctorSpecialization);
        
        // Calculate platform fee
        BigDecimal platformFeePercentage = systemConfigService.getPlatformFeePercentage();
        BigDecimal platformFee = consultationFee.multiply(platformFeePercentage);
        BigDecimal doctorAmount = consultationFee.subtract(platformFee);
        
        // Create payment record with PENDING status
        Payment payment = Payment.builder()
            .patientId(dto.getPatientId())
            .doctorId(dto.getDoctorId())
            .caseId(dto.getCaseId())
            .appointmentId(dto.getAppointmentId())
            .paymentType(PaymentType.CONSULTATION)
            .amount(consultationFee)
            .platformFee(platformFee)
            .doctorAmount(doctorAmount)
            .status(PaymentStatus.PENDING)
            .paymentMethod(PaymentMethod.STRIPE)
            .currency("USD")
            .idempotencyKey(idempotencyKey)
            .createdBy(dto.getPatientId())
            .build();
        
        // Add metadata
        payment.addMetadata("doctor_specialization", doctorSpecialization);
        payment.addMetadata("case_id", String.valueOf(dto.getCaseId()));
        payment.addMetadata("patient_id", String.valueOf(dto.getPatientId()));
        payment.addMetadata("doctor_id", String.valueOf(dto.getDoctorId()));
        
        // Create Stripe payment intent
        Map<String, String> stripeMetadata = new HashMap<>();
        stripeMetadata.put("payment_type", "CONSULTATION");
        stripeMetadata.put("case_id", String.valueOf(dto.getCaseId()));
        stripeMetadata.put("patient_id", String.valueOf(dto.getPatientId()));
        stripeMetadata.put("doctor_id", String.valueOf(dto.getDoctorId()));
        stripeMetadata.put("platform_fee", platformFee.toString());
        
        PaymentIntent paymentIntent = stripePaymentGateway.createPaymentIntent(
            consultationFee, 
            "USD", 
            stripeMetadata, 
            idempotencyKey
        );
        
        // Update payment with Stripe details
        payment.setStripePaymentIntentId(paymentIntent.getId());
        payment.setTransactionId(generateTransactionId());
        
        // Save payment
        payment = paymentRepository.save(payment);
        
        log.info("Initiated consultation payment for case {} with payment intent {}", 
                dto.getCaseId(), paymentIntent.getId());
        
        // Return payment intent details for frontend
        return PaymentIntentDto.builder()
            .paymentId(payment.getId())
            .paymentIntentId(paymentIntent.getId())
            .clientSecret(paymentIntent.getClientSecret())
            .amount(consultationFee)
            .currency("USD")
            .status(payment.getStatus().toString())
            .build();
    }
    
    /**
     * Confirm consultation payment
     */
    @Transactional
    public Payment confirmConsultationPayment(Long paymentId, String paymentMethodId) 
            throws StripePaymentException {
        
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException("Payment is not in pending status");
        }
        
        // Confirm payment with Stripe
        PaymentIntent confirmedIntent = stripePaymentGateway.confirmPaymentIntent(
            payment.getStripePaymentIntentId(), 
            paymentMethodId
        );
        
        // Update payment based on intent status
        if ("succeeded".equals(confirmedIntent.getStatus())) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setProcessedAt(LocalDateTime.now());
            payment.setStripePaymentMethodId(paymentMethodId);
            
            // Get charge details
            if (confirmedIntent.getLatestChargeObject() != null) {
                Charge charge = confirmedIntent.getLatestChargeObject();
                payment.setStripeChargeId(charge.getId());
                payment.setReceiptUrl(charge.getReceiptUrl());
                
                // Calculate Stripe fee (usually 2.9% + 30¢)
                BalanceTransaction bt = charge.getBalanceTransaction() != null ?
                        charge.getBalanceTransactionObject() : null;
                        Long stripeFeeInCents = bt != null ?
                                bt.getFee() : 0L;
                payment.setStripeFee(new BigDecimal(stripeFeeInCents).divide(new BigDecimal(100)));
                payment.setNetAmount(payment.getAmount().subtract(payment.getStripeFee()));
            }
            
            payment.setGatewayResponse("Payment successful");
            
            // Send success event
            paymentEventProducer.sendPaymentCompletedEvent(payment);
            
            log.info("Consultation payment confirmed for payment ID: {}", paymentId);
            
        } else if ("processing".equals(confirmedIntent.getStatus())) {
            payment.setStatus(PaymentStatus.PROCESSING);
            payment.setGatewayResponse("Payment is processing");
            
        } else if ("requires_action".equals(confirmedIntent.getStatus())) {
            payment.setStatus(PaymentStatus.PENDING);
            payment.setGatewayResponse("Additional action required");
            
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setGatewayResponse("Payment failed: " + confirmedIntent.getStatus());
            
            // Send failure event
            paymentEventProducer.sendPaymentFailedEvent(payment);
        }
        
        return paymentRepository.save(payment);
    }
    
    /**
     * Cancel a pending payment
     */
    @Transactional
    public Payment cancelPayment(Long paymentId, String reason) throws StripePaymentException {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException("Only pending payments can be canceled");
        }
        
        // Cancel Stripe payment intent
        stripePaymentGateway.cancelPaymentIntent(payment.getStripePaymentIntentId());
        
        // Update payment status
        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setGatewayResponse("Payment canceled: " + reason);
        payment.addMetadata("cancellation_reason", reason);
        
        payment = paymentRepository.save(payment);
        
        // Send cancellation event
        paymentEventProducer.sendPaymentCancelledEvent(payment);
        
        log.info("Payment canceled for payment ID: {}", paymentId);
        
        return payment;
    }
    
    /**
     * Handle payment webhook from Stripe
     */
    @Transactional
    public void handlePaymentIntentWebhook(String paymentIntentId, String status) {
        paymentRepository.findByStripePaymentIntentId(paymentIntentId)
            .ifPresent(payment -> {
                PaymentStatus oldStatus = payment.getStatus();
                
                switch (status) {
                    case "succeeded":
                        if (payment.getStatus() != PaymentStatus.COMPLETED) {
                            payment.setStatus(PaymentStatus.COMPLETED);
                            payment.setProcessedAt(LocalDateTime.now());
                            paymentEventProducer.sendPaymentCompletedEvent(payment);
                        }
                        break;
                        
                    case "payment_failed":
                        payment.setStatus(PaymentStatus.FAILED);
                        paymentEventProducer.sendPaymentFailedEvent(payment);
                        break;
                        
                    case "canceled":
                        payment.setStatus(PaymentStatus.CANCELLED);
                        paymentEventProducer.sendPaymentCancelledEvent(payment);
                        break;
                        
                    case "processing":
                        payment.setStatus(PaymentStatus.PROCESSING);
                        break;
                }
                
                if (oldStatus != payment.getStatus()) {
                    paymentRepository.save(payment);
                    log.info("Payment {} status updated from {} to {} via webhook", 
                            payment.getId(), oldStatus, payment.getStatus());
                }
            });
    }
    
    /**
     * Retry a failed payment
     */
    @Transactional
    public PaymentIntentDto retryFailedPayment(Long paymentId) throws StripePaymentException {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        if (payment.getStatus() != PaymentStatus.FAILED) {
            throw new RuntimeException("Only failed payments can be retried");
        }
        
        // Create new idempotency key for retry
        String newIdempotencyKey = generateIdempotencyKey(payment, "retry");
        
        // Create new payment intent
        Map<String, String> metadata = new HashMap<>();
        metadata.put("retry_of_payment_id", String.valueOf(paymentId));
        metadata.put("payment_type", payment.getPaymentType().toString());
        metadata.put("case_id", String.valueOf(payment.getCaseId()));
        
        PaymentIntent paymentIntent = stripePaymentGateway.createPaymentIntent(
            payment.getAmount(),
            payment.getCurrency(),
            metadata,
            newIdempotencyKey
        );
        
        // Update payment record
        payment.setStripePaymentIntentId(paymentIntent.getId());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setIdempotencyKey(newIdempotencyKey);
        payment.addMetadata("retry_count", 
            String.valueOf(Integer.parseInt(payment.getMetadata().getOrDefault("retry_count", "0")) + 1));
        
        paymentRepository.save(payment);
        
        log.info("Retrying failed payment {} with new payment intent {}", 
                paymentId, paymentIntent.getId());
        
        return PaymentIntentDto.builder()
            .paymentId(payment.getId())
            .paymentIntentId(paymentIntent.getId())
            .clientSecret(paymentIntent.getClientSecret())
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .status(payment.getStatus().toString())
            .build();
    }
    
    /**
     * Generate unique transaction ID
     */
    private String generateTransactionId() {
        return "TXN_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Generate idempotency key for payment
     */
    private String generateIdempotencyKey(ProcessConsultationPaymentDto dto) {
        return String.format("consultation_%d_%d_%d_%d", 
            dto.getCaseId(), dto.getPatientId(), dto.getDoctorId(), System.currentTimeMillis());
    }
    
    private String generateIdempotencyKey(Payment payment, String suffix) {
        return String.format("payment_%d_%s_%d", 
            payment.getId(), suffix, System.currentTimeMillis());
    }
}