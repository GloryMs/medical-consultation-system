package com.supervisorservice.exception;

/**
 * Exception thrown when coupon is invalid or cannot be used
 */
public class InvalidCouponException extends SupervisorServiceException {
    
    public InvalidCouponException(String message) {
        super(message, "INVALID_COUPON");
    }
}
