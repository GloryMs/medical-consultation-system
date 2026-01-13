package com.commonlibrary.dto.coupon;

import com.commonlibrary.entity.BeneficiaryType;
import com.commonlibrary.entity.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Kafka event payload for coupon distribution.
 * Published by admin-service when a coupon is distributed to a beneficiary.
 * Consumed by supervisor-service and patient-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponDistributionEvent {
    
    /**
     * Event type identifier
     */
    private String eventType;
    
    /**
     * Coupon ID in admin-service
     */
    private Long couponId;
    
    /**
     * Unique coupon code
     */
    private String couponCode;
    
    /**
     * Type of beneficiary receiving the coupon
     */
    private BeneficiaryType beneficiaryType;
    
    /**
     * Beneficiary ID (supervisor or patient)
     */
    private Long beneficiaryId;
    
    /**
     * Type of discount
     */
    private DiscountType discountType;
    
    /**
     * Discount value
     */
    private BigDecimal discountValue;
    
    /**
     * Maximum discount amount (for percentage)
     */
    private BigDecimal maxDiscountAmount;
    
    /**
     * Currency code
     */
    private String currency;
    
    /**
     * When the coupon expires
     */
    private LocalDateTime expiresAt;
    
    /**
     * Whether coupon can be transferred between patients
     */
    private Boolean isTransferable;
    
    /**
     * Batch ID if part of batch distribution
     */
    private Long batchId;
    
    /**
     * Batch code if part of batch distribution
     */
    private String batchCode;
    
    /**
     * Admin user ID who distributed
     */
    private Long distributedBy;
    
    /**
     * Notes about the distribution
     */
    private String notes;
    
    /**
     * When this event was generated
     */
    private LocalDateTime timestamp;
}