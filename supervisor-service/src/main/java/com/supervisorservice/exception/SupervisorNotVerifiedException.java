package com.supervisorservice.exception;

/**
 * Exception thrown when supervisor is not verified
 */
public class SupervisorNotVerifiedException extends SupervisorServiceException {
    
    public SupervisorNotVerifiedException(String message) {
        super(message, "SUPERVISOR_NOT_VERIFIED");
    }
}
