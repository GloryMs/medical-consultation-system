package com.supervisorservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for coupon redemption result
 */
@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponRedemptionDto {

    private Long couponId;

    private String couponCode;

    private Long caseId;

    private Long patientId;

    private Long supervisorId;

    private BigDecimal originalAmount;

    private BigDecimal discountAmount;

    private BigDecimal finalAmount;

    private String transactionId;

    private Long paymentId;

    private LocalDateTime redeemedAt;

    private String message;
}