package com.supervisorservice.exception;

/**
 * Exception thrown when supervisor doesn't have access to patient
 */
public class UnauthorizedPatientAccessException extends SupervisorServiceException {
    
    public UnauthorizedPatientAccessException(String message) {
        super(message, "UNAUTHORIZED_PATIENT_ACCESS");
    }
}
