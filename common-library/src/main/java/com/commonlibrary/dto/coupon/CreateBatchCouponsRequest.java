package com.commonlibrary.dto.coupon;

import com.commonlibrary.entity.BeneficiaryType;
import com.commonlibrary.entity.DiscountType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating a batch of coupons by admin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBatchCouponsRequest {
    
    /**
     * Custom batch code prefix (optional - will be auto-generated if not provided)
     */
    private String batchCodePrefix;
    
    /**
     * Number of coupons to create in this batch
     */
    @NotNull(message = "Total coupons is required")
    @Min(value = 1, message = "Must create at least 1 coupon")
    @Max(value = 1000, message = "Cannot create more than 1000 coupons in a batch")
    private Integer totalCoupons;
    
    /**
     * Type of discount for all coupons in batch
     */
    @NotNull(message = "Discount type is required")
    private DiscountType discountType;
    
    /**
     * Discount value for all coupons (percentage 1-100 or fixed amount)
     */
    @NotNull(message = "Discount value is required")
    @Positive(message = "Discount value must be positive")
    private BigDecimal discountValue;
    
    /**
     * Maximum discount amount (optional, for percentage discounts)
     */
    private BigDecimal maxDiscountAmount;
    
    /**
     * Currency code (default: USD)
     */
    @Builder.Default
    private String currency = "USD";
    
    /**
     * Type of beneficiary for all coupons
     */
    @NotNull(message = "Beneficiary type is required")
    private BeneficiaryType beneficiaryType;
    
    /**
     * Beneficiary ID (optional - can distribute later if null)
     */
    private Long beneficiaryId;
    
    /**
     * Number of days until expiration (from creation or distribution)
     */
    @NotNull(message = "Expiry days is required")
    @Min(value = 1, message = "Expiry must be at least 1 day")
    @Max(value = 730, message = "Expiry cannot exceed 2 years (730 days)")
    @Builder.Default
    private Integer expiryDays = 180;
    
    /**
     * Whether coupons can be transferred between patients by supervisor
     */
    @Builder.Default
    private Boolean isTransferable = true;
    
    /**
     * Notes about the batch
     */
    private String notes;
    
    /**
     * Whether to auto-distribute all coupons to beneficiary after creation
     */
    @Builder.Default
    private Boolean autoDistribute = false;
}