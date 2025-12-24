package com.adminservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Request DTO for assigning a case to a doctor
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignCaseRequest {
    
    @NotNull(message = "Case ID is required")
    private Long caseId;
    
    @NotNull(message = "Doctor ID is required")
    private Long doctorId;
    
    private String priority; // PRIMARY, SECONDARY, BACKUP
    
    private String notes; // Optional assignment notes
}