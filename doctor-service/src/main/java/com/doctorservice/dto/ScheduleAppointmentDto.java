package com.doctorservice.dto;

import com.commonlibrary.entity.ConsultationType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduleAppointmentDto {
    @NotNull(message = "Case ID is required")
    private Long caseId;

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Scheduled time is required")
    private LocalDateTime scheduledTime;

    private Integer duration = 30; // default 30 minutes

    @NotNull(message = "Consultation type is required")
    private ConsultationType consultationType;
}
