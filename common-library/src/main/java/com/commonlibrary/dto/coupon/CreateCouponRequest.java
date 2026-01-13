package com.commonlibrary.dto.coupon;

import com.commonlibrary.entity.BeneficiaryType;
import com.commonlibrary.entity.DiscountType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO for creating a single coupon by admin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCouponRequest {
    
    /**
     * Custom coupon code (optional - will be auto-generated if not provided)
     */
    private String couponCode;
    
    /**
     * Type of discount
     */
    @NotNull(message = "Discount type is required")
    private DiscountType discountType;
    
    /**
     * Discount value (percentage 1-100 or fixed amount)
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
     * Type of beneficiary
     */
    @NotNull(message = "Beneficiary type is required")
    private BeneficiaryType beneficiaryType;
    
    /**
     * Beneficiary ID (optional - can distribute later if null)
     */
    private Long beneficiaryId;
    
    /**
     * Expiration date/time
     */
    @NotNull(message = "Expiration date is required")
    @Future(message = "Expiration date must be in the future")
    private LocalDateTime expiresAt;
    
    /**
     * Whether the coupon can be transferred between patients by supervisor
     */
    @Builder.Default
    private Boolean isTransferable = true;
    
    /**
     * Notes about the coupon
     */
    private String notes;
    
    /**
     * Whether to auto-distribute to beneficiary after creation
     */
    @Builder.Default
    private Boolean autoDistribute = false;
}