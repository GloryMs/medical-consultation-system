package com.commonlibrary;

/**
 * Status of supervisor coupons
 */
public enum CouponStatus {
    /**
     * Coupon is available for use
     */
    AVAILABLE,
    
    /**
     * Coupon has been used
     */
    USED,
    
    /**
     * Coupon has expired
     */
    EXPIRED,
    
    /**
     * Coupon was cancelled/revoked
     */
    CANCELLED
}