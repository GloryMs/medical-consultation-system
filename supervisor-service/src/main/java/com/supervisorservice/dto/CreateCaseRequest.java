package com.supervisorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for creating a case via supervisor-service
 * Files can be uploaded separately after case creation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCaseRequest {
    private String caseTitle;
    private String description;
    private String primaryDiseaseCode;
    private List<String> secondaryDiseaseCodes;
    private List<String> symptomCodes;
    private List<String> currentMedicationCodes;
    private String requiredSpecialization;
    private List<String> secondarySpecializations;
    private String urgencyLevel;
    private String complexity;
    private Boolean requiresSecondOpinion;
    private Integer minDoctorsRequired;
    private Integer maxDoctorsAllowed;
    private Long dependentId;
    private Long supervisorId;
}