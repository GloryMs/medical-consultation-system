package com.supervisorservice.exception;

/**
 * Exception thrown when a resource is not found
 */
public class ResourceNotFoundException extends SupervisorServiceException {
    
    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND");
    }
    
    public ResourceNotFoundException(String resource, String identifier) {
        super(String.format("%s not found with identifier: %s", resource, identifier), "RESOURCE_NOT_FOUND");
    }
}
