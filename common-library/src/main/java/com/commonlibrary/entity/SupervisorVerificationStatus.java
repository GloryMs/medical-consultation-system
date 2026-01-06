package com.commonlibrary.entity;

/**
 * Verification status for medical supervisors
 */
public enum SupervisorVerificationStatus {
    /**
     * Initial state when supervisor registers
     */
    PENDING,
    
    /**
     * Supervisor has been verified by admin
     */
    VERIFIED,
    
    /**
     * Supervisor application was rejected
     */
    REJECTED,
    
    /**
     * Supervisor account has been suspended
     */
    SUSPENDED
}
