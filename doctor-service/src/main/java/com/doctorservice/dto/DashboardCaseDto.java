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
public class DashboardCaseDto {
    private Long id;
    private String patientName;
    private String caseTitle;
    private String status;
    private String urgencyLevel;
    private LocalDateTime submittedAt;
    private String requiredSpecialization;
    private String nextAction;
}