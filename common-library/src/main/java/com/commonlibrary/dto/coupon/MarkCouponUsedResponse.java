package com.commonlibrary.dto.coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for marking a coupon as used.
 * Returned by admin-service after successfully marking a coupon as used.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkCouponUsedResponse {
    
    /**
     * Whether the operation was successful
     */
    private Boolean success;
    
    /**
     * The coupon ID that was marked as used
     */
    private Long couponId;
    
    /**
     * The coupon code
     */
    private String couponCode;
    
    /**
     * When the coupon was marked as used
     */
    private LocalDateTime usedAt;
    
    /**
     * Message describing the result
     */
    private String message;
    
    /**
     * Error code if operation failed
     */
    private String errorCode;

    private Long paymentId;
}