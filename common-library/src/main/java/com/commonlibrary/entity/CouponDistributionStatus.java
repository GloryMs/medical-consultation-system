package com.commonlibrary.entity;

/**
 * Status of coupon distribution from admin to beneficiary.
 * Tracks the acknowledgment and acceptance of distributed coupons.
 */
public enum CouponDistributionStatus {
    
    /**
     * Coupon has been distributed but not yet acknowledged by beneficiary.
     * Waiting for beneficiary to confirm receipt.
     */
    PENDING,
    
    /**
     * Beneficiary has acknowledged receipt of the coupon.
     * Coupon is now available for use by the beneficiary.
     */
    ACKNOWLEDGED,
    
    /**
     * Beneficiary has rejected the coupon distribution.
     * Coupon returns to admin pool for redistribution.
     */
    REJECTED,
    
    /**
     * Distribution expired before beneficiary could acknowledge.
     * Coupon returns to admin pool for redistribution.
     */
    EXPIRED,
    
    /**
     * Distribution was cancelled by admin before acknowledgment.
     * Coupon returns to admin pool.
     */
    CANCELLED
}