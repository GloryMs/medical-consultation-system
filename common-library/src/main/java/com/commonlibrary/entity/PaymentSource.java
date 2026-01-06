package com.commonlibrary.entity;

/**
 * Source of payment for case consultations
 */
public enum PaymentSource {
    /**
     * Direct payment via Stripe
     */
    DIRECT,
    
    /**
     * Payment via coupon redemption
     */
    COUPON
}
