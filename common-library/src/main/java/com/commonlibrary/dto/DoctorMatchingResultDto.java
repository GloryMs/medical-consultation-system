package com.commonlibrary.dto;

import com.commonlibrary.entity.AssignmentPriority;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class DoctorMatchingResultDto {
    private DoctorDto doctor; // Legacy compatibility
    private DoctorCapacityDto doctorCapacity; // New capacity information
    private Double totalScore;
    private Map<String, Double> scoreBreakdown;
    private String matchingReason;
    private AssignmentPriority priority;

    // Workload-specific information
    private Double workloadPercentage;
    private Boolean emergencyMode;
    private String workloadNotes;
    private Boolean canAcceptImmediately;

    // Predicted assignment success
    private Double assignmentSuccessProbability;
    private String riskFactors;

    public boolean isHighWorkloadRisk() {
        return workloadPercentage > 85.0 && !emergencyMode;
    }

    public boolean isOptimalChoice() {
        return totalScore > 70.0 && workloadPercentage < 70.0;
    }
}