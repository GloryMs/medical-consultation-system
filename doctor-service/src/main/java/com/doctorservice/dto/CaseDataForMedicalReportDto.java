package com.doctorservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CaseDataForMedicalReportDto {
    private Long appointmentId;
    private Long doctorId;
    private Long caseId;
    private Long patientId;
    private String patientName;
    private String doctorName;
    private String caseTitle;
    private String caseDescription;
    private String caseRequiredSpecialization;
    private LocalDateTime caseSubmittedAt;
}
