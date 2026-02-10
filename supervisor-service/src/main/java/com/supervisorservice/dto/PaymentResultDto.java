package com.supervisorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for payment result after coupon redemption or direct payment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResultDto {
    
    private Long paymentId;
    private Long caseId;
    private Long patientId;
    private Long supervisorId;
    private String paymentSource; // DIRECT or COUPON
    private BigDecimal amount;
    private String currency;
    private String status;
    
    // For coupon payments
    private Long couponId;
    private String couponCode;
    
    // For direct payments
    private String stripePaymentIntentId;
    private String stripeClientSecret;
    
    private LocalDateTime timestamp;
    private String message;
}
