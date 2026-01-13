package com.supervisorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for payment operations.
 * Contains payment details and status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {

    private Long paymentId;
    private Long caseId;
    private Long patientId;
    private Long supervisorId;
    private Long doctorId;
    private Long appointmentId;

    private String paymentSource; // STRIPE, PAYPAL, COUPON
    
    private BigDecimal amount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String currency;
    
    private String status; // PENDING, COMPLETED, FAILED, REFUNDED

    // Coupon details (if applicable)
    private Long couponId;
    private String couponCode;

    // Stripe details (if applicable)
    private String stripePaymentIntentId;
    private String stripeClientSecret;

    // PayPal details (if applicable)
    private String paypalOrderId;

    private LocalDateTime timestamp;
    private String message;
}