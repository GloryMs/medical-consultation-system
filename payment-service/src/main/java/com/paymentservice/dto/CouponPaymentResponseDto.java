package com.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for coupon-based payments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponPaymentResponseDto {

    private boolean success;

    private Long paymentId;

    private String transactionId;

    private Long caseId;

    private Long patientId;

    private Long doctorId;

    private Long couponId;

    private String couponCode;

    /**
     * Original consultation fee before discount
     */
    private BigDecimal originalAmount;

    /**
     * Discount amount applied from coupon
     */
    private BigDecimal discountAmount;

    /**
     * Final amount after discount (may be 0 for full coverage)
     */
    private BigDecimal finalAmount;

    /**
     * Platform fee (calculated on original amount)
     */
    private BigDecimal platformFee;

    /**
     * Doctor's share (after platform fee)
     */
    private BigDecimal doctorAmount;

    private String currency;

    private String status;

    private LocalDateTime processedAt;

    private String message;

    private String errorCode;
}