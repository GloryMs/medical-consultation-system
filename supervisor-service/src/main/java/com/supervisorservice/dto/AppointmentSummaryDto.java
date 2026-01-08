package com.supervisorservice.dto;

import lombok.*;

import java.util.Map;

/**
 * DTO for appointment summary statistics
 */
@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentSummaryDto {

    private Long totalAppointments;
    private Long upcomingAppointments;
    private Long completedAppointments;
    private Long cancelledAppointments;
    private Long rescheduledAppointments;
    private Long pendingRescheduleRequests;
    private Map<String, Long> appointmentsByStatus;
    private Map<String, Long> appointmentsByPatient;
}