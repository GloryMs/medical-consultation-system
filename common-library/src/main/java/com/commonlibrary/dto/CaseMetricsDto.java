package com.commonlibrary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for case metrics and statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseMetricsDto {
    
    // Overall counts
    private Long totalCases;
    private Long activeCases;
    private Long closedCases;
    
    // Status breakdown
    private Long submittedCount;
    private Long pendingCount;
    private Long assignedCount;
    private Long acceptedCount;
    private Long scheduledCount;
    private Long paymentPendingCount;
    private Long inProgressCount;
    private Long consultationCompleteCount;
    private Long rejectedCount;
    
    // Urgency breakdown
    private Long lowUrgencyCount;
    private Long mediumUrgencyCount;
    private Long highUrgencyCount;
    private Long criticalUrgencyCount;
    
    // Time metrics
    private Double averageAssignmentTime; // Hours from submission to assignment
    private Double averageResolutionTime; // Days from submission to closure
    private Double averageResponseTime; // Hours for doctor to respond to assignment
    
    // Assignment metrics
    private Long unassignedCasesCount;
    private Long overdueAssignmentsCount;
    private Double assignmentSuccessRate; // Percentage of cases successfully assigned
    
    // Specialization breakdown
    private Map<String, Long> casesBySpecialization;
    
    // Monthly trends
    private Map<String, Long> casesPerMonth; // Format: "2024-01" -> count
    
    // Status distribution percentages
    private Map<String, Double> statusDistribution;
}