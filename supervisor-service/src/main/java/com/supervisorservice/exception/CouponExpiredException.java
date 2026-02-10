package com.supervisorservice.exception;

/**
 * Exception thrown when coupon has expired
 */
public class CouponExpiredException extends SupervisorServiceException {
    
    public CouponExpiredException(String message) {
        super(message, "COUPON_EXPIRED");
    }
}
