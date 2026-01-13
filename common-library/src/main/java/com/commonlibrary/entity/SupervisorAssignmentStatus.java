package com.commonlibrary.entity;

/**
 * Status of patient assignment to supervisor
 */
public enum SupervisorAssignmentStatus {
    /**
     * Assignment is currently active
     */
    ACTIVE,
    
    /**
     * Assignment has been temporarily suspended
     */
    SUSPENDED,
    
    /**
     * Assignment has been permanently terminated
     */
    TERMINATED,

    REMOVED,
    AVAILABLE,
    ASSIGNED
}
