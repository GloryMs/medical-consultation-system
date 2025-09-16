package com.doctorservice.dto;

import com.commonlibrary.entity.ConsultationType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduleAppointmentDto {
    private Long caseId;
    private Long patientId;
    private LocalDateTime scheduledTime;
    private Integer duration = 30; // default 30 minutes
    private ConsultationType consultationType;
}
