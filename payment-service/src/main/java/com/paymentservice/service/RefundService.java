package com.paymentservice.service;

import com.commonlibrary.entity.PaymentStatus;
import com.paymentservice.dto.RefundRequestDto;
import com.paymentservice.dto.RefundResponseDto;
import com.paymentservice.entity.Payment;
import com.paymentservice.entity.RefundLog;
import com.paymentservice.exception.StripePaymentException;
import com.paymentservice.kafka.PaymentEventProducer;
import com.paymentservice.repository.PaymentRepository;
import com.paymentservice.repository.RefundLogRepository;
import com.paymentservice.service.stripe.StripePaymentGateway;
import com.stripe.model.Refund;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for processing refunds
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {
    
    private final StripePaymentGateway stripePaymentGateway;
    private final PaymentRepository paymentRepository;
    private final RefundLogRepository refundLogRepository;
    private final DoctorBalanceService doctorBalanceService;
    private final PaymentEventProducer paymentEventProducer;
    
    /**
     * Process refund for a payment
     */
    @Transactional
    public RefundResponseDto processRefund(RefundRequestDto refundRequest) throws StripePaymentException {
        
        // Validate payment
        Payment payment = paymentRepository.findById(refundRequest.getPaymentId())
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new RuntimeException("Can only refund completed payments");
        }
        
        if (payment.getRefundedAt() != null) {
            throw new RuntimeException("Payment has already been refunded");
        }
        
        // Determine refund amount
        BigDecimal refundAmount = refundRequest.getAmount() != null ? 
            refundRequest.getAmount() : payment.getAmount();
        
        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new RuntimeException("Refund amount cannot exceed payment amount");
        }
        
        // Create refund log
        RefundLog refundLog = RefundLog.builder()
            .paymentId(payment.getId())
            .refundAmount(refundAmount)
            .refundReason(refundRequest.getReason())
            .refundType(refundRequest.getRefundType())
            .initiatedBy(refundRequest.getInitiatedBy())
            .initiatorRole(refundRequest.getInitiatorRole())
            .status("PENDING")
            .build();
        
        refundLog = refundLogRepository.save(refundLog);
        
        try {
            // Process refund with Stripe
            Map<String, String> metadata = new HashMap<>();
            metadata.put("payment_id", String.valueOf(payment.getId()));
            metadata.put("refund_reason", refundRequest.getReason());
            metadata.put("refund_type", refundRequest.getRefundType());
            metadata.put("case_id", String.valueOf(payment.getCaseId()));
            
            Refund stripeRefund = stripePaymentGateway.createRefund(
                payment.getStripeChargeId(),
                refundAmount,
                mapRefundReason(refundRequest.getRefundType()),
                metadata
            );
            
            // Update payment record
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAt(LocalDateTime.now());
            payment.setRefundReason(refundRequest.getReason());
            payment.setRefundAmount(refundAmount);
            payment.setStripeRefundId(stripeRefund.getId());
            
            // Calculate refund fees
            BigDecimal refundFee = calculateRefundFee(payment, refundAmount);
            payment.setRefundFee(refundFee);
            
            // Update doctor balance if consultation fee refund
            if (payment.getDoctorId() != null) {
                handleDoctorBalanceForRefund(payment, refundAmount, refundFee);
            }
            
            paymentRepository.save(payment);
            
            // Update refund log
            refundLog.setStatus("COMPLETED");
            refundLog.setStripeRefundId(stripeRefund.getId());
            refundLog.setProcessedAt(LocalDateTime.now());
            refundLog.setRefundFee(refundFee);
            refundLogRepository.save(refundLog);
            
            // Send refund event
            paymentEventProducer.sendRefundProcessedEvent(payment, refundAmount);
            
            log.info("Refund processed successfully for payment {}: ${}", 
                    payment.getId(), refundAmount);
            
            return RefundResponseDto.builder()
                .refundId(refundLog.getId())
                .paymentId(payment.getId())
                .refundAmount(refundAmount)
                .refundFee(refundFee)
                .status("COMPLETED")
                .stripeRefundId(stripeRefund.getId())
                .processedAt(LocalDateTime.now())
                .message("Refund processed successfully")
                .build();
            
        } catch (Exception e) {
            // Update refund log with failure
            refundLog.setStatus("FAILED");
            refundLog.setErrorMessage(e.getMessage());
            refundLogRepository.save(refundLog);
            
            log.error("Failed to process refund for payment {}", payment.getId(), e);
            throw new StripePaymentException("Failed to process refund: " + e.getMessage(), e);
        }
    }
    
    /**
     * Process doctor no-show refund
     */
    @Transactional
    public RefundResponseDto processDoctorNoShowRefund(Long caseId, Long adminId) 
            throws StripePaymentException {
        
        // Find payment for the case
        List<Payment> payments = paymentRepository.findByCaseId(caseId).stream()
            .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
            .collect(Collectors.toList());
        
        if (payments.isEmpty()) {
            throw new RuntimeException("No completed payment found for case");
        }
        
        Payment payment = payments.get(0); // Get most recent completed payment
        
        RefundRequestDto refundRequest = RefundRequestDto.builder()
            .paymentId(payment.getId())
            .amount(payment.getAmount()) // Full refund
            .reason("Doctor no-show")
            .refundType("DOCTOR_NO_SHOW")
            .initiatedBy(adminId)
            .initiatorRole("ADMIN")
            .build();
        
        return processRefund(refundRequest);
    }
    
    /**
     * Process incomplete consultation refund
     */
    @Transactional
    public RefundResponseDto processIncompleteConsultationRefund(Long caseId, String reason, Long adminId) 
            throws StripePaymentException {
        
        // Find payment for the case
        List<Payment> payments = paymentRepository.findByCaseId(caseId).stream()
            .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
            .collect(Collectors.toList());
        
        if (payments.isEmpty()) {
            throw new RuntimeException("No completed payment found for case");
        }
        
        Payment payment = payments.get(0);
        
        RefundRequestDto refundRequest = RefundRequestDto.builder()
            .paymentId(payment.getId())
            .amount(payment.getAmount()) // Full refund
            .reason(reason)
            .refundType("INCOMPLETE_CONSULTATION")
            .initiatedBy(adminId)
            .initiatorRole("ADMIN")
            .build();
        
        return processRefund(refundRequest);
    }
    
    /**
     * Process partial refund
     */
    @Transactional
    public RefundResponseDto processPartialRefund(Long paymentId, BigDecimal amount, 
                                                  String reason, Long adminId) 
            throws StripePaymentException {
        
        RefundRequestDto refundRequest = RefundRequestDto.builder()
            .paymentId(paymentId)
            .amount(amount)
            .reason(reason)
            .refundType("PARTIAL_REFUND")
            .initiatedBy(adminId)
            .initiatorRole("ADMIN")
            .build();
        
        return processRefund(refundRequest);
    }
    
    /**
     * Get refund history for a payment
     */
    public List<RefundLog> getRefundHistory(Long paymentId) {
        return refundLogRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId);
    }
    
    /**
     * Get all refunds within date range
     */
    public List<RefundLog> getRefundsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return refundLogRepository.findByProcessedAtBetween(startDate, endDate);
    }
    
    /**
     * Calculate refund fee (Stripe fees that doctor must bear)
     */
    private BigDecimal calculateRefundFee(Payment payment, BigDecimal refundAmount) {
        // Stripe doesn't refund their processing fee (2.9% + 30¢)
        // This fee will be deducted from doctor's balance
        
        BigDecimal percentageFee = refundAmount.multiply(new BigDecimal("0.029"));
        BigDecimal fixedFee = new BigDecimal("0.30");
        
        // For partial refunds, calculate proportional fee
        if (refundAmount.compareTo(payment.getAmount()) < 0) {
            BigDecimal refundRatio = refundAmount.divide(payment.getAmount(), 4, BigDecimal.ROUND_HALF_UP);
            fixedFee = fixedFee.multiply(refundRatio);
        }
        
        return percentageFee.add(fixedFee);
    }
    
    /**
     * Handle doctor balance adjustment for refund
     */
    private void handleDoctorBalanceForRefund(Payment payment, BigDecimal refundAmount, 
                                             BigDecimal refundFee) {
        // Doctor bears the refund amount from their earnings plus the Stripe fee
        BigDecimal totalDeduction = payment.getDoctorAmount(); // Original doctor earnings
        
        // Add the non-refundable Stripe fee to doctor's deduction
        totalDeduction = totalDeduction.add(refundFee);
        
        // Update doctor's balance
        doctorBalanceService.deductFromBalance(payment.getDoctorId(), totalDeduction, 
            "Refund for payment " + payment.getId());
        
        log.info("Deducted {} from doctor {} balance for refund", totalDeduction, payment.getDoctorId());
    }
    
    /**
     * Map refund type to Stripe refund reason
     */
    private String mapRefundReason(String refundType) {
        switch (refundType) {
            case "DOCTOR_NO_SHOW":
            case "INCOMPLETE_CONSULTATION":
                return "requested_by_customer";
            case "DUPLICATE_PAYMENT":
                return "duplicate";
            case "FRAUDULENT":
                return "fraudulent";
            default:
                return "requested_by_customer";
        }
    }
    
    /**
     * Get refund statistics
     */
    public Map<String, Object> getRefundStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        List<RefundLog> refunds = refundLogRepository.findByProcessedAtBetween(startDate, endDate);
        
        BigDecimal totalRefunded = refunds.stream()
            .filter(r -> "COMPLETED".equals(r.getStatus()))
            .map(RefundLog::getRefundAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalFees = refunds.stream()
            .filter(r -> "COMPLETED".equals(r.getStatus()))
            .map(RefundLog::getRefundFee)
            .filter(fee -> fee != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Map<String, Long> refundsByType = refunds.stream()
            .collect(Collectors.groupingBy(RefundLog::getRefundType, Collectors.counting()));
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRefunds", refunds.size());
        stats.put("completedRefunds", refunds.stream().filter(r -> "COMPLETED".equals(r.getStatus())).count());
        stats.put("failedRefunds", refunds.stream().filter(r -> "FAILED".equals(r.getStatus())).count());
        stats.put("totalRefundedAmount", totalRefunded);
        stats.put("totalRefundFees", totalFees);
        stats.put("refundsByType", refundsByType);
        
        return stats;
    }
}