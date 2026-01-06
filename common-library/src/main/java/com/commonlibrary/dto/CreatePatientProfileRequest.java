package com.commonlibrary.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for supervisor to create a patient profile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePatientProfileRequest {
    
    // Basic Information
    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;
    
    @NotBlank(message = "Date of birth is required")
    private String dateOfBirth; // Format: YYYY-MM-DD
    
    @NotBlank(message = "Gender is required")
    private String gender; // MALE, FEMALE, OTHER
    
    // Address Information
    @NotBlank(message = "Address is required")
    private String address;
    
    @NotBlank(message = "City is required")
    private String city;
    
    @NotBlank(message = "Country is required")
    private String country;
    
    private String state;
    private String zipCode;
    
    // Medical Information
    private String bloodType;
    private String allergies;
    private String chronicConditions;
    private String currentMedications;
    
    // Emergency Contact
    @NotBlank(message = "Emergency contact name is required")
    private String emergencyContactName;
    
    @NotBlank(message = "Emergency contact phone is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid emergency contact phone format")
    private String emergencyContactPhone;
    
    private String emergencyContactRelationship;
    
    // Additional Information
    private String notes; // Supervisor's notes about the patient
    private Boolean autoAssignToSupervisor = true; // Automatically assign to creating supervisor
}