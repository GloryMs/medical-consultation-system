package com.doctorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAppointmentDto {
    private Long id;
    private String patientName;
    private LocalDateTime scheduledTime;
    private String consultationType;
    private String status;
    private Long caseId;
    private String urgencyLevel;
    private Integer duration;
}