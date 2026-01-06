package com.commonlibrary.entity;

/**
 * Status of supervisor coupons
 */
public enum CouponStatus {
    /**
     * Coupon is available for use
     */
    AVAILABLE,
    
    /**
     * Coupon has been used for a case payment
     */
    USED,
    
    /**
     * Coupon has expired
     */
    EXPIRED,
    
    /**
     * Coupon has been manually cancelled
     */
    CANCELLED
}
