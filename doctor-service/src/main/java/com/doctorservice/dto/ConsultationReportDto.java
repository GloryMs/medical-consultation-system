package com.doctorservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConsultationReportDto {
    //Appointment Info
    @NotNull(message = "Appointment ID is required")
    private Long appointmentId;
    private LocalDateTime appointmentTime;

    private String diagnosis;
    private String recommendations;
    private String prescriptions;
    private String followUpInstructions;
    private Boolean requiresFollowUp;
    private LocalDateTime nextAppointmentSuggested;
    private String doctorNotes;

    //Case Info:
    @NotNull(message = "Case ID is required")
    private Long caseId;
    private String caseTitle;

    //Doctor Info:
    private String doctorName;
    private String doctorEmail;
    private String doctorPhone;
    private String doctorSpecialization;

    //Patient Info:
    private String patientName;
    private String patientEmail;
    private String patientPhone;


}
