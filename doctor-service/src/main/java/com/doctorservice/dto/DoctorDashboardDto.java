package com.doctorservice.dto;

import com.commonlibrary.dto.NotificationDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDashboardDto {
    private DoctorDashboardStatsDto stats;
    private List<DashboardCaseDto> recentCases;
    private List<DashboardAppointmentDto> upcomingAppointments;
    private List<NotificationDto> recentNotifications;
}