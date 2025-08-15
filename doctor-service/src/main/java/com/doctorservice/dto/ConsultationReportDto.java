package com.doctorservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConsultationReportDto {
    @NotNull(message = "Appointment ID is required")
    private Long appointmentId;

    @NotNull(message = "Case ID is required")
    private Long caseId;

    private String diagnosis;
    private String recommendations;
    private String prescriptions;
    private String followUpInstructions;
    private Boolean requiresFollowUp;
    private LocalDateTime nextAppointmentSuggested;
    private String doctorNotes;
}
