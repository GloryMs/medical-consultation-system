package com.supervisorservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for supervisor payment operations
 */
@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupervisorPaymentResponseDto {

    private boolean success;

    private Long paymentId;

    private Long caseId;

    private Long patientId;

    private Long doctorId;

    private Long supervisorId;

    private String paymentMethod;

    /**
     * Original consultation fee amount
     */
    private BigDecimal amount;

    /**
     * Discount amount (if coupon used)
     */
    private BigDecimal discountAmount;

    /**
     * Final amount charged after discounts
     */
    private BigDecimal finalAmount;

    /**
     * Coupon code used (if applicable)
     */
    private String couponCode;

    /**
     * Stripe payment intent ID (if Stripe payment)
     */
    private String stripePaymentIntentId;

    /**
     * Stripe charge ID (if Stripe payment)
     */
    private String stripeChargeId;

    /**
     * PayPal order ID (if PayPal payment)
     */
    private String paypalOrderId;

    /**
     * Internal transaction ID
     */
    private String transactionId;

    /**
     * Receipt URL for the payment
     */
    private String receiptUrl;

    /**
     * When the payment was processed
     */
    private LocalDateTime processedAt;

    /**
     * Status message
     */
    private String message;

    /**
     * Error details if payment failed
     */
    private String errorDetails;
}