package com.adminservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for reassigning a case to a different doctor
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReassignCaseRequest {
    
    @NotNull(message = "New doctor ID is required")
    private Long newDoctorId;
    
    @NotBlank(message = "Reassignment reason is required")
    private String reason; // Required reason for audit trail
    
    private String priority; // PRIMARY, SECONDARY, BACKUP
}