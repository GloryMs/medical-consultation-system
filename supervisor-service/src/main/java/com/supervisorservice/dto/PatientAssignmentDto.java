package com.supervisorservice.dto;

import com.commonlibrary.entity.SupervisorAssignmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for patient assignment information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientAssignmentDto {
    
    private Long id;
    private Long supervisorId;
    private Long patientId;
    private String patientName;
    private SupervisorAssignmentStatus assignmentStatus;
    private LocalDateTime assignedAt;
    private Long assignedBy;
    private String assignmentNotes;
    private LocalDateTime terminatedAt;
    private String terminationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Patient details (from Feign client)
    private String patientFirstName;
    private String patientLastName;
    private String patientEmail;
    private String patientPhoneNumber;
}
