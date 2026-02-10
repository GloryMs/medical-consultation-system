package com.supervisorservice.exception;

/**
 * Exception thrown for validation errors
 */
public class ValidationException extends SupervisorServiceException {
    
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR");
    }
}
