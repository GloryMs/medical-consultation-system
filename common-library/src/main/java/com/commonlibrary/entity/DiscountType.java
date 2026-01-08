package com.commonlibrary.entity;

/**
 * Types of discounts for supervisor coupons
 */
public enum DiscountType {
    /**
     * Percentage discount (e.g., 20% off)
     */
    PERCENTAGE,
    
    /**
     * Fixed amount discount (e.g., $50 off)
     */
    FIXED_AMOUNT,
    
    /**
     * Full coverage - covers entire consultation fee
     */
    FULL_COVERAGE
}