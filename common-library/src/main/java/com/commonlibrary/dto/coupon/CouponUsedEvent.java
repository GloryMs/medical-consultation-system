package com.commonlibrary.dto.coupon;

import com.commonlibrary.entity.BeneficiaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Kafka event payload for coupon usage/redemption.
 * Published by admin-service when a coupon is marked as used.
 * Consumed by supervisor-service and payment-service for local status updates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponUsedEvent {
    
    /**
     * Event type identifier
     */
    private String eventType;
    
    /**
     * Coupon ID in admin-service
     */
    private Long couponId;
    
    /**
     * Coupon code that was used
     */
    private String couponCode;
    
    /**
     * Type of beneficiary who redeemed
     */
    private BeneficiaryType beneficiaryType;
    
    /**
     * Beneficiary ID (supervisor or patient)
     */
    private Long beneficiaryId;
    
    /**
     * Patient ID the coupon was used for
     */
    private Long patientId;
    
    /**
     * Case ID the coupon was used for
     */
    private Long caseId;
    
    /**
     * Payment ID created for this transaction
     */
    private Long paymentId;
    
    /**
     * Original consultation fee
     */
    private BigDecimal originalAmount;
    
    /**
     * Discount amount applied
     */
    private BigDecimal discountAmount;
    
    /**
     * Amount charged via payment gateway (if any)
     */
    private BigDecimal chargedAmount;
    
    /**
     * Currency code
     */
    private String currency;
    
    /**
     * When the coupon was used
     */
    private LocalDateTime usedAt;
    
    /**
     * User ID who redeemed the coupon
     */
    private Long redeemedByUserId;
    
    /**
     * When this event was generated
     */
    private LocalDateTime timestamp;
}