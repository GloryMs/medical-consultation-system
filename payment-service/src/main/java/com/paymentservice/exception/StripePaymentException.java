package com.paymentservice.exception;

/**
 * Base exception for Stripe payment operations
 */
public class StripePaymentException extends Exception {
    
    private String stripeErrorCode;
    private String stripeErrorType;
    
    public StripePaymentException(String message) {
        super(message);
    }
    
    public StripePaymentException(String message, Throwable cause) {
        super(message, cause);
        
        if (cause instanceof com.stripe.exception.StripeException) {
            com.stripe.exception.StripeException stripeException = 
                (com.stripe.exception.StripeException) cause;
            this.stripeErrorCode = stripeException.getCode();
            this.stripeErrorType = stripeException.getClass().getSimpleName();
        }
    }
    
    public String getStripeErrorCode() {
        return stripeErrorCode;
    }
    
    public String getStripeErrorType() {
        return stripeErrorType;
    }
}