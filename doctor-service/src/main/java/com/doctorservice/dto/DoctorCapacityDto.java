package com.doctorservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DoctorCapacityDto {
    private Long doctorId;
    private String fullName;
    private String primarySpecialization;
    private Integer activeCases;
    private Integer maxActiveCases;
    private Integer todayAppointments;
    private Integer maxDailyAppointments;
    private Double workloadPercentage;
    private Double rating;
    private Integer consultationCount;
    private Boolean isAvailable;
    private Boolean emergencyMode;
    private Double matchingScore; // For case assignment algorithm
}