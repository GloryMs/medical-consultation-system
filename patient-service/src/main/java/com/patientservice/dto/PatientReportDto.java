package com.patientservice.dto;

import com.patientservice.entity.Case;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PatientReportDto {
    private String patientName;
    private LocalDate dateOfBirth;
    private String bloodGroup;
    private String allergies;
    private String chronicConditions;
    private String medicalHistory;
    private Integer totalCases;
    private List<Case> cases;
    private LocalDateTime generatedAt;
}
