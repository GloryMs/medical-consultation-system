package com.adminservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for filtering cases in admin panel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseFilterDto {
    private String status; // PENDING, ASSIGNED, ACCEPTED, etc.
    private String urgencyLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private String specialization; // Required specialization code
    private Long patientId; // Filter by specific patient
    private Long doctorId; // Filter by assigned doctor
    private String startDate; // Format: yyyy-MM-dd
    private String endDate; // Format: yyyy-MM-dd
    private String searchTerm; // Search in case title/description
}