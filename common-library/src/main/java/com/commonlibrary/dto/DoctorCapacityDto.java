package com.commonlibrary.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class DoctorCapacityDto {
    // Basic doctor information
    private Long doctorId;
    private String fullName;
    private String primarySpecialization;
    private Set<String> subSpecializations;
    
    // Workload information
    private Integer activeCases;
    private Integer maxActiveCases;
    private Integer todayAppointments;
    private Integer maxDailyAppointments;
    private Double workloadPercentage;
    
    // Availability information
    private Boolean isAvailable;
    private Boolean emergencyMode;
    private String emergencyModeReason;
    private LocalDateTime nextAvailableSlot;
    
    // Performance metrics
    private Double averageRating;
    private Integer consultationCount;
    private Double completionRate;
    
    // Real-time status
    private LocalDateTime lastWorkloadUpdate;
    private Boolean hasCapacityForNewCase;
    private Boolean hasCapacityForUrgentCase;
    
    // Additional metadata for matching
    private Double matchingScore; // Calculated by assignment algorithm
    private String availabilityNotes;
    private List<String> specializedDiseases; // Diseases doctor specializes in
    private Integer yearsOfExperience;
    
    // Capacity predictions
    private Double predictedWorkloadIn24Hours;
    private Integer estimatedAvailableSlots;
    
    public boolean canAcceptCase() {
        return isAvailable && (emergencyMode || hasCapacityForNewCase);
    }
    
    public boolean canAcceptUrgentCase() {
        return isAvailable && (emergencyMode || hasCapacityForUrgentCase || workloadPercentage < 80.0);
    }
    
    public boolean isOverloaded() {
        return workloadPercentage > 85.0;
    }
    
    public boolean isLightlyLoaded() {
        return workloadPercentage < 50.0;
    }
}