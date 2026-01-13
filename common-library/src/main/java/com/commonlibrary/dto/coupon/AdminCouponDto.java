package com.commonlibrary.dto.coupon;

import com.commonlibrary.entity.AdminCouponStatus;
import com.commonlibrary.entity.BeneficiaryType;
import com.commonlibrary.entity.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing a coupon from admin-service.
 * Used for transferring coupon data between services.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCouponDto {
    
    /**
     * Coupon ID in admin-service
     */
    private Long id;
    
    /**
     * Unique coupon code
     */
    private String couponCode;
    
    /**
     * Type of discount
     */
    private DiscountType discountType;
    
    /**
     * Discount value (percentage or fixed amount)
     */
    private BigDecimal discountValue;
    
    /**
     * Maximum discount amount (for percentage with cap)
     */
    private BigDecimal maxDiscountAmount;
    
    /**
     * Currency code
     */
    private String currency;
    
    /**
     * Type of beneficiary
     */
    private BeneficiaryType beneficiaryType;
    
    /**
     * Beneficiary ID (supervisor or patient)
     */
    private Long beneficiaryId;
    
    /**
     * Beneficiary name (for display)
     */
    private String beneficiaryName;
    
    /**
     * Current status
     */
    private AdminCouponStatus status;
    
    /**
     * Batch ID if part of a batch
     */
    private Long batchId;
    
    /**
     * Batch code if part of a batch
     */
    private String batchCode;
    
    /**
     * Admin user ID who created the coupon
     */
    private Long createdBy;
    
    /**
     * Admin user ID who distributed the coupon
     */
    private Long distributedBy;
    
    /**
     * When the coupon was distributed
     */
    private LocalDateTime distributedAt;
    
    /**
     * When the coupon was used (if used)
     */
    private LocalDateTime usedAt;
    
    /**
     * Case ID the coupon was used for (if used)
     */
    private Long usedForCaseId;
    
    /**
     * Payment ID the coupon was used for (if used)
     */
    private Long usedForPaymentId;
    
    /**
     * Patient ID the coupon was used for (if used)
     */
    private Long usedByPatientId;
    
    /**
     * When the coupon expires
     */
    private LocalDateTime expiresAt;
    
    /**
     * Whether the coupon is expiring soon (within 30 days)
     */
    private Boolean isExpiringSoon;
    
    /**
     * Days until expiry
     */
    private Integer daysUntilExpiry;
    
    /**
     * Whether the coupon can be transferred between patients (by supervisor)
     */
    private Boolean isTransferable;
    
    /**
     * Notes about the coupon
     */
    private String notes;
    
    /**
     * Cancellation reason (if cancelled)
     */
    private String cancellationReason;
    
    /**
     * When the coupon was cancelled (if cancelled)
     */
    private LocalDateTime cancelledAt;
    
    /**
     * Admin user ID who cancelled the coupon (if cancelled)
     */
    private Long cancelledBy;
    
    /**
     * When the coupon was created
     */
    private LocalDateTime createdAt;
    
    /**
     * When the coupon was last updated
     */
    private LocalDateTime updatedAt;
}