package com.commonlibrary.dto;

import com.commonlibrary.entity.TimeSlot;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class AvailabilityStatusDto {
    private Long doctorId;
    private Boolean isAvailable;
    private Boolean emergencyMode;
    private String emergencyReason;
    private Set<TimeSlot> availableTimeSlots;
    private Integer maxActiveCases;
    private Integer maxDailyAppointments;
    private Integer currentActiveCases;
    private Integer todayAppointments;
    private Double workloadPercentage;
    private LocalDateTime lastUpdated;
    private Long totalWeeklyWorkingHours;
}