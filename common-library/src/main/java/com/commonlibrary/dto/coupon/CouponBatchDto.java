package com.commonlibrary.dto.coupon;

import com.commonlibrary.entity.BeneficiaryType;
import com.commonlibrary.entity.CouponBatchStatus;
import com.commonlibrary.entity.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing a coupon batch.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponBatchDto {
    
    /**
     * Batch ID
     */
    private Long id;
    
    /**
     * Unique batch code
     */
    private String batchCode;
    
    /**
     * Type of beneficiary for all coupons
     */
    private BeneficiaryType beneficiaryType;
    
    /**
     * Beneficiary ID (if assigned)
     */
    private Long beneficiaryId;
    
    /**
     * Beneficiary name (for display)
     */
    private String beneficiaryName;
    
    /**
     * Total number of coupons in batch
     */
    private Integer totalCoupons;
    
    /**
     * Number of available coupons
     */
    private Integer availableCoupons;
    
    /**
     * Number of used coupons
     */
    private Integer usedCoupons;
    
    /**
     * Number of expired coupons
     */
    private Integer expiredCoupons;
    
    /**
     * Number of cancelled coupons
     */
    private Integer cancelledCoupons;
    
    /**
     * Type of discount for all coupons
     */
    private DiscountType discountType;
    
    /**
     * Discount value
     */
    private BigDecimal discountValue;
    
    /**
     * Maximum discount amount
     */
    private BigDecimal maxDiscountAmount;
    
    /**
     * Currency code
     */
    private String currency;
    
    /**
     * Days until expiration
     */
    private Integer expiryDays;
    
    /**
     * Batch status
     */
    private CouponBatchStatus status;
    
    /**
     * Admin user ID who created the batch
     */
    private Long createdBy;
    
    /**
     * Admin user ID who distributed the batch
     */
    private Long distributedBy;
    
    /**
     * When the batch was distributed
     */
    private LocalDateTime distributedAt;
    
    /**
     * Notes about the batch
     */
    private String notes;
    
    /**
     * When the batch was created
     */
    private LocalDateTime createdAt;
    
    /**
     * When the batch was last updated
     */
    private LocalDateTime updatedAt;
}