package com.commonlibrary.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a user account via auth-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    private String email;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Role is required")
    private String role; // PATIENT, DOCTOR, ADMIN, SUPERVISOR

    @NotBlank(message = "Password is required")
    private String password;

    private String phoneNumber;
    private Boolean createdBySupervisor;
    private Long supervisorId;
}