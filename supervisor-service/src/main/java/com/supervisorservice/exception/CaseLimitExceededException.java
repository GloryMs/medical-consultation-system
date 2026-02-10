package com.supervisorservice.exception;

/**
 * Exception thrown when supervisor has reached active case limit for a patient
 */
public class CaseLimitExceededException extends SupervisorServiceException {
    
    public CaseLimitExceededException(String message) {
        super(message, "CASE_LIMIT_EXCEEDED");
    }
}
