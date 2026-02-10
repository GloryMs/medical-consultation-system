package com.commonlibrary.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User entity representation for inter-service communication
 * Note: This is a simplified version - full User entity exists in auth-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;
    private String email;
    private String fullName;
    private String role;
    private String status;
    private Boolean emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;

    // Supervisor-related fields
    private Boolean createdBySupervisor;
    private Long supervisorId;
}