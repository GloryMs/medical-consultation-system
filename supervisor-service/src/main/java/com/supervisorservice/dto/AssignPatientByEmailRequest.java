package com.supervisorservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for assigning an existing patient by email
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignPatientByEmailRequest {
    
    @NotBlank(message = "Patient email is required")
    @Email(message = "Invalid email format")
    private String patientEmail;
    
    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String assignmentNotes;
    
    // Optional: For verification
    private String patientFullName; // To verify correct patient
}