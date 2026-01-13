package com.commonlibrary.dto;

import com.commonlibrary.entity.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for patient-facing coupon allocation data.
 * Used when patients view their available coupons (from supervisor assignments).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientCouponAllocationDto {

    private Long id;

    /**
     * Coupon code for display and use
     */
    private String couponCode;

    /**
     * Patient ID who owns this coupon
     */
    private Long patientId;

    // ==================== Discount Info ====================

    /**
     * Type of discount
     */
    private DiscountType discountType;

    /**
     * Discount value (percentage or fixed amount)
     */
    private BigDecimal discountValue;

    /**
     * Maximum discount amount (for percentage discounts)
     */
    private BigDecimal maxDiscountAmount;

    /**
     * Currency
     */
    private String currency;

    // ==================== Status ====================

    /**
     * Whether the coupon is available for use
     */
    private boolean available;

    /**
     * When the coupon was assigned to the patient
     */
    private LocalDateTime assignedAt;

    /**
     * When the coupon expires
     */
    private LocalDateTime expiresAt;

    /**
     * Whether the coupon is expiring soon (within 30 days)
     */
    private boolean expiringSoon;

    /**
     * Days until expiry
     */
    private int daysUntilExpiry;

    // ==================== Usage Info ====================

    /**
     * Whether the coupon has been used
     */
    private boolean used;

    /**
     * When the coupon was used
     */
    private LocalDateTime usedAt;

    /**
     * Case ID the coupon was used for
     */
    private Long usedForCaseId;

    // ==================== Display Helpers ====================

    /**
     * Human-readable discount description
     */
    public String getDiscountDescription() {
        if (discountType == null) return "Unknown";
        
        switch (discountType) {
            case PERCENTAGE:
                String desc = discountValue.intValue() + "% off";
                if (maxDiscountAmount != null) {
                    desc += " (max " + currency + " " + maxDiscountAmount + ")";
                }
                return desc;
            case FIXED_AMOUNT:
                return currency + " " + discountValue + " off";
            case FULL_COVERAGE:
                return "Full coverage";
            default:
                return "Discount";
        }
    }

    /**
     * Calculate discount for a given amount
     */
    public BigDecimal calculateDiscount(BigDecimal amount) {
        if (amount == null || discountType == null) return BigDecimal.ZERO;
        
        switch (discountType) {
            case PERCENTAGE:
                BigDecimal discount = amount.multiply(discountValue).divide(BigDecimal.valueOf(100));
                if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
                    return maxDiscountAmount;
                }
                return discount;
            case FIXED_AMOUNT:
                return discountValue.min(amount);
            case FULL_COVERAGE:
                return amount;
            default:
                return BigDecimal.ZERO;
        }
    }
}