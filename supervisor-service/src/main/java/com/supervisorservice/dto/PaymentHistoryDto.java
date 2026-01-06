package com.supervisorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for payment history information from payment-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryDto {
    private Long id;
    private String transactionId;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private Long caseId;
    private String paymentType; // CONSULTATION, SUBSCRIPTION
    private BigDecimal amount;
    private BigDecimal platformFee;
    private BigDecimal doctorAmount;
    private String currency;
    private String paymentMethod; // CREDIT_CARD, DEBIT_CARD, PAYPAL, etc.
    private String status; // PENDING, COMPLETED, FAILED, REFUNDED
    private String description;
    private LocalDateTime processedAt;
    private LocalDateTime refundedAt;
    private String refundReason;
    private LocalDateTime createdAt;
}