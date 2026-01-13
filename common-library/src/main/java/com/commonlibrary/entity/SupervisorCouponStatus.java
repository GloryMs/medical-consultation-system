package com.commonlibrary.entity;

/**
 * Status of coupons in supervisor-service (local allocation tracking).
 * This enum tracks the coupon lifecycle from the supervisor's perspective.
 */
public enum SupervisorCouponStatus {
    
    /**
     * Coupon has been received by supervisor but not assigned to any patient.
     * Supervisor can assign this coupon to one of their managed patients.
     */
    AVAILABLE,
    
    /**
     * Coupon has been assigned to a specific patient by the supervisor.
     * Ready to be used for the patient's case payment.
     */
    ASSIGNED,
    
    /**
     * Coupon has been successfully used for a case payment.
     * This is a terminal state - synced from admin-service.
     */
    USED,
    
    /**
     * Coupon has expired before being used.
     * This is a terminal state - synced from admin-service.
     */
    EXPIRED,
    
    /**
     * Coupon has been cancelled by admin.
     * This is a terminal state - synced from admin-service.
     */
    CANCELLED,
    
    /**
     * Coupon status is being synced with admin-service.
     * Temporary state during synchronization.
     */
    SYNCING
}