package com.supervisorservice.exception;

/**
 * Exception thrown when duplicate resource is found
 */
public class DuplicateResourceException extends SupervisorServiceException {
    
    public DuplicateResourceException(String message) {
        super(message, "DUPLICATE_RESOURCE");
    }
}
