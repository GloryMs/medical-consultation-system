package com.supervisorservice.exception;

/**
 * Exception thrown when supervisor has reached patient limit
 */
public class PatientLimitExceededException extends SupervisorServiceException {
    
    public PatientLimitExceededException(String message) {
        super(message, "PATIENT_LIMIT_EXCEEDED");
    }
}
