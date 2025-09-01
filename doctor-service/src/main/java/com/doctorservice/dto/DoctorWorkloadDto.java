package com.doctorservice.dto;

import com.doctorservice.entity.Appointment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DoctorWorkloadDto {
    private Long doctorId;
    private Boolean isAvailable;
    private Integer activeCases;
    private Integer maxActiveCases;
    private Integer todayAppointments;
    private Integer maxDailyAppointments;
    private Integer thisWeekAppointments;
    private Integer consultationCount;
    private Double averageRating;
    private Double workloadPercentage;
    private LocalDateTime nextAvailableSlot;
    private List<Appointment> upcomingAppointments;
    private Boolean emergencyMode;
    private String emergencyModeReason;
    private LocalDateTime lastWorkloadUpdate;
}