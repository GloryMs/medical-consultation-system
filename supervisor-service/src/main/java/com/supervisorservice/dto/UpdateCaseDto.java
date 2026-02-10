package com.supervisorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for updating case information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCaseDto {
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
}