package com.commonlibrary.dto.coupon;

import com.commonlibrary.entity.BeneficiaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Kafka event payload for coupon expiration.
 * Published by admin-service scheduled job when coupons expire.
 * Consumed by supervisor-service and patient-service to update local status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponExpiredEvent {
    
    /**
     * Event type identifier
     */
    private String eventType;
    
    /**
     * List of expired coupon IDs
     */
    private List<Long> couponIds;
    
    /**
     * List of expired coupon codes
     */
    private List<String> couponCodes;
    
    /**
     * Type of beneficiary (for filtering by consumers)
     */
    private BeneficiaryType beneficiaryType;
    
    /**
     * Beneficiary ID (for filtering by consumers)
     */
    private Long beneficiaryId;
    
    /**
     * When the coupons expired
     */
    private LocalDateTime expiredAt;
    
    /**
     * When this event was generated
     */
    private LocalDateTime timestamp;
}