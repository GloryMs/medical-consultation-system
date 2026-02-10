package com.supervisorservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating supervisor profile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSupervisorProfileRequest {
    
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    private String fullName;
    
    @Size(max = 255, message = "Organization name must not exceed 255 characters")
    private String organizationName;
    
    @Size(max = 100, message = "Organization type must not exceed 100 characters")
    private String organizationType;
    
    @Size(max = 100, message = "License number must not exceed 100 characters")
    private String licenseNumber;
    
    @Size(max = 50, message = "Phone number must not exceed 50 characters")
    private String phoneNumber;
    
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;
    
    private String address;
    
    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;
    
    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;
}
