package com.doctorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDashboardStatsDto {
    private Integer activeCases;
    private Integer todayAppointments;
    private Integer totalConsultations;
    private Double avgRating;
    private Double workloadPercentage;
    private Double totalEarnings;
    private Integer pendingReports;
    private Integer unreadNotifications;
}