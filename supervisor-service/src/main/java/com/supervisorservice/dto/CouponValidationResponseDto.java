package com.supervisorservice.dto;

import com.commonlibrary.entity.DiscountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for coupon validation
 */
@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponValidationResponseDto {

    /**
     * Whether the coupon is valid for use
     */
    private boolean valid;

    /**
     * Coupon ID in database
     */
    private Long couponId;

    /**
     * Coupon code
     */
    private String couponCode;

    /**
     * Type of discount: PERCENTAGE, FIXED_AMOUNT, FULL_COVERAGE
     */
    private DiscountType discountType;

    /**
     * Discount value (percentage or fixed amount)
     */
    private BigDecimal discountValue;

    /**
     * Calculated discount amount based on consultation fee
     */
    private BigDecimal discountAmount;

    /**
     * Remaining amount after discount (0 if fully covered)
     */
    private BigDecimal remainingAmount;

    /**
     * Original consultation fee
     */
    private BigDecimal originalAmount;

    /**
     * When the coupon expires
     */
    private LocalDateTime expiresAt;

    /**
     * Patient ID the coupon is assigned to (null if any patient)
     */
    private Long patientId;

    /**
     * Validation message
     */
    private String message;
}