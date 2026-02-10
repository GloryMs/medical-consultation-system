package com.supervisorservice.exception;

/**
 * Exception thrown when coupon has already been used
 */
public class CouponAlreadyUsedException extends SupervisorServiceException {
    
    public CouponAlreadyUsedException(String message) {
        super(message, "COUPON_ALREADY_USED");
    }
}
