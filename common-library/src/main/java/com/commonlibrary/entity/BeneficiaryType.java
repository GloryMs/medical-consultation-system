package com.commonlibrary.entity;

/**
 * Types of beneficiaries who can receive coupons from admin.
 * Used in the coupon management system to identify who the coupon is issued to.
 */
public enum BeneficiaryType {
    
    /**
     * Coupon issued to a Medical Supervisor.
     * Supervisor can then assign the coupon to their managed patients.
     */
    MEDICAL_SUPERVISOR,
    
    /**
     * Coupon issued directly to a Patient.
     * Patient can use the coupon for their own case payments.
     */
    PATIENT
}