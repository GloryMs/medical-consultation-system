package com.commonlibrary.dto.coupon;

import com.commonlibrary.entity.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for coupon validation.
 * Returned by admin-service after validating a coupon.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponValidationResponse {
    
    /**
     * Whether the coupon is valid and can be used
     */
    private Boolean valid;
    
    /**
     * The coupon ID in admin-service (if valid)
     */
    private Long couponId;
    
    /**
     * The coupon code that was validated
     */
    private String couponCode;
    
    /**
     * Type of discount: PERCENTAGE, FIXED_AMOUNT, or FULL_COVERAGE
     */
    private DiscountType discountType;
    
    /**
     * The discount value (percentage or fixed amount)
     */
    private BigDecimal discountValue;
    
    /**
     * Maximum discount amount (for percentage discounts with cap)
     */
    private BigDecimal maxDiscountAmount;
    
    /**
     * Calculated discount amount to apply
     */
    private BigDecimal discountAmount;
    
    /**
     * Amount remaining to be paid after discount
     */
    private BigDecimal remainingAmount;
    
    /**
     * Original consultation fee
     */
    private BigDecimal originalAmount;
    
    /**
     * Currency of the coupon
     */
    private String currency;
    
    /**
     * When the coupon expires
     */
    private LocalDateTime expiresAt;
    
    /**
     * Patient ID the coupon is valid for (if assigned)
     */
    private Long patientId;
    
    /**
     * Beneficiary ID the coupon belongs to
     */
    private Long beneficiaryId;
    
    /**
     * Validation message (success or error reason)
     */
    private String message;
    
    /**
     * Error code if validation failed
     */
    private String errorCode;
}