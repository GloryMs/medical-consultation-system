package com.commonlibrary.constants;

/**
 * Constants for coupon-related error codes.
 * Used across all services for standardized error responses.
 */
public final class CouponErrorCodes {
    
    private CouponErrorCodes() {
        // Prevent instantiation
    }
    
    // ==================== Validation Errors ====================
    
    /**
     * Coupon code not found
     */
    public static final String COUPON_NOT_FOUND = "COUPON_001";
    
    /**
     * Coupon has already been used
     */
    public static final String COUPON_ALREADY_USED = "COUPON_002";
    
    /**
     * Coupon has expired
     */
    public static final String COUPON_EXPIRED = "COUPON_003";
    
    /**
     * Coupon has been cancelled
     */
    public static final String COUPON_CANCELLED = "COUPON_004";
    
    /**
     * Coupon is not assigned to this patient
     */
    public static final String COUPON_PATIENT_MISMATCH = "COUPON_005";
    
    /**
     * Coupon does not belong to this beneficiary
     */
    public static final String COUPON_BENEFICIARY_MISMATCH = "COUPON_006";
    
    /**
     * Coupon is not yet distributed
     */
    public static final String COUPON_NOT_DISTRIBUTED = "COUPON_007";
    
    /**
     * Coupon is suspended
     */
    public static final String COUPON_SUSPENDED = "COUPON_008";
    
    // ==================== Distribution Errors ====================
    
    /**
     * Coupon already distributed to another beneficiary
     */
    public static final String COUPON_ALREADY_DISTRIBUTED = "COUPON_101";
    
    /**
     * Beneficiary not found
     */
    public static final String BENEFICIARY_NOT_FOUND = "COUPON_102";
    
    /**
     * Invalid beneficiary type
     */
    public static final String INVALID_BENEFICIARY_TYPE = "COUPON_103";
    
    // ==================== Assignment Errors ====================
    
    /**
     * Coupon already assigned to a patient
     */
    public static final String COUPON_ALREADY_ASSIGNED = "COUPON_201";
    
    /**
     * Patient not assigned to this supervisor
     */
    public static final String PATIENT_NOT_ASSIGNED_TO_SUPERVISOR = "COUPON_202";
    
    /**
     * Coupon not transferable
     */
    public static final String COUPON_NOT_TRANSFERABLE = "COUPON_203";
    
    // ==================== Redemption Errors ====================
    
    /**
     * Case not found for redemption
     */
    public static final String CASE_NOT_FOUND = "COUPON_301";
    
    /**
     * Case not in valid status for payment
     */
    public static final String CASE_INVALID_STATUS = "COUPON_302";
    
    /**
     * Payment processing failed
     */
    public static final String PAYMENT_FAILED = "COUPON_303";
    
    /**
     * Consultation fee not set for case
     */
    public static final String FEE_NOT_SET = "COUPON_304";
    
    // ==================== Batch Errors ====================
    
    /**
     * Batch not found
     */
    public static final String BATCH_NOT_FOUND = "COUPON_401";
    
    /**
     * Batch already distributed
     */
    public static final String BATCH_ALREADY_DISTRIBUTED = "COUPON_402";
    
    /**
     * Batch is empty
     */
    public static final String BATCH_EMPTY = "COUPON_403";

    // ==================== Coupon Validation Errors ====================

    /**
     * Coupon validation failed
     */
    public static final String COUPON_VALIDATION_FAILED = "COUPON_501";
    
    // ==================== System Errors ====================
    
    /**
     * Service communication error
     */
    public static final String SERVICE_COMMUNICATION_ERROR = "COUPON_901";
    
    /**
     * Database error
     */
    public static final String DATABASE_ERROR = "COUPON_902";
    
    /**
     * Unexpected error
     */
    public static final String UNEXPECTED_ERROR = "COUPON_999";
}