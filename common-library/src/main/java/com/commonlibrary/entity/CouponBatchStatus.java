package com.commonlibrary.entity;

/**
 * Status of coupon batches created by admin.
 * Tracks the lifecycle of batch coupon operations.
 */
public enum CouponBatchStatus {
    
    /**
     * Batch has been created but coupons not yet distributed.
     * All coupons in batch are in CREATED status.
     */
    CREATED,
    
    /**
     * Batch coupons have been distributed to beneficiary.
     * All coupons in batch are in DISTRIBUTED status.
     */
    DISTRIBUTED,
    
    /**
     * Some coupons in batch have been used.
     * At least one coupon is USED, others may be DISTRIBUTED.
     */
    PARTIALLY_USED,
    
    /**
     * All coupons in batch have been used.
     * All coupons are in USED status.
     */
    FULLY_USED,
    
    /**
     * All coupons in batch have expired.
     * All coupons are in EXPIRED status.
     */
    EXPIRED,
    
    /**
     * Batch has been cancelled by admin.
     * All coupons in batch are in CANCELLED status.
     */
    CANCELLED
}