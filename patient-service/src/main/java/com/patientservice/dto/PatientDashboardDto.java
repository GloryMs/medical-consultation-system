package com.patientservice.dto;

import java.util.List;

import com.commonlibrary.dto.AppointmentDto;
import com.commonlibrary.dto.NotificationDto;
import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PatientDashboardDto {
    StatsDto stats;
    List<CaseDto> recentCases;
    List<AppointmentDto> upcomingAppointments;
    List<NotificationDto> recentNotifications;
}
