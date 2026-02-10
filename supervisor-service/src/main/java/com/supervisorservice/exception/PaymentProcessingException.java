package com.supervisorservice.exception;

/**
 * Exception thrown when payment processing fails
 */
public class PaymentProcessingException extends SupervisorServiceException {
    
    public PaymentProcessingException(String message) {
        super(message, "PAYMENT_PROCESSING_ERROR");
    }
    
    public PaymentProcessingException(String message, Throwable cause) {
        super(message, "PAYMENT_PROCESSING_ERROR", cause);
    }
}
