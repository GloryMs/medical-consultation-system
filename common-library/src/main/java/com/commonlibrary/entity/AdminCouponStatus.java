package com.commonlibrary.entity;

/**
 * Status of coupons managed by admin-service (master/source of truth).
 * This enum tracks the complete lifecycle of a coupon from creation to usage.
 */
public enum AdminCouponStatus {
    
    /**
     * Coupon has been created but not yet distributed to any beneficiary.
     * Coupon exists in the pool and can be distributed by admin.
     */
    CREATED,
    
    /**
     * Coupon has been distributed to a beneficiary (Supervisor or Patient).
     * The beneficiary has received the coupon and can use it.
     */
    DISTRIBUTED,
    
    /**
     * Coupon has been successfully redeemed for a case payment.
     * This is a terminal state - coupon cannot be used again.
     */
    USED,
    
    /**
     * Coupon has passed its expiration date without being used.
     * This is a terminal state - coupon cannot be used.
     */
    EXPIRED,
    
    /**
     * Coupon has been manually cancelled by an admin.
     * This is a terminal state - coupon cannot be used.
     */
    CANCELLED,
    
    /**
     * Coupon is temporarily suspended (e.g., pending investigation).
     * Can be reactivated to DISTRIBUTED status by admin.
     */
    SUSPENDED
}