package com.doctorservice.dto;

import com.doctorservice.entity.Appointment;
import com.doctorservice.entity.Doctor;
import com.doctorservice.entity.ReportStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConsultationReportDto {

    private Long id;
    private Long appointmentId;
    private Long doctorId;
    private Long caseId;
    private Long patientId;
    private String diagnosis;
    private String recommendations;
    private String prescriptions;
    private String followUpInstructions;
    private Boolean requiresFollowUp = false;
    private LocalDateTime nextAppointmentSuggested;
    private String doctorNotes;
    private ReportStatus status = ReportStatus.DRAFT;
    private String pdfFileLink;
    private LocalDateTime exportedAt;
    private LocalDateTime finalizedAt;
    private String doctorName;
    private String patientName;

}
