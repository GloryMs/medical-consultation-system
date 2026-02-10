package com.supervisorservice.exception;

/**
 * Base exception for supervisor service
 */
public class SupervisorServiceException extends RuntimeException {
    
    private String errorCode;
    
    public SupervisorServiceException(String message) {
        super(message);
    }
    
    public SupervisorServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public SupervisorServiceException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public SupervisorServiceException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
