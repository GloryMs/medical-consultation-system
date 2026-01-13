package com.commonlibrary.dto.coupon;

import com.commonlibrary.entity.BeneficiaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Kafka event payload for coupon cancellation.
 * Published by admin-service when a coupon is cancelled.
 * Consumed by supervisor-service and patient-service to update local status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponCancelledEvent {
    
    /**
     * Event type identifier
     */
    private String eventType;
    
    /**
     * Coupon ID in admin-service
     */
    private Long couponId;
    
    /**
     * Coupon code that was cancelled
     */
    private String couponCode;
    
    /**
     * Type of beneficiary who had the coupon
     */
    private BeneficiaryType beneficiaryType;
    
    /**
     * Beneficiary ID (supervisor or patient)
     */
    private Long beneficiaryId;
    
    /**
     * Reason for cancellation
     */
    private String cancellationReason;
    
    /**
     * Admin user ID who cancelled
     */
    private Long cancelledBy;
    
    /**
     * When the coupon was cancelled
     */
    private LocalDateTime cancelledAt;
    
    /**
     * When this event was generated
     */
    private LocalDateTime timestamp;
}