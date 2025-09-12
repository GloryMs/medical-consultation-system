package com.patientservice.dto;

import com.commonlibrary.entity.CaseComplexity;
import com.commonlibrary.entity.UrgencyLevel;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Validated
public class CreateCaseDto {

    private String caseTitle;
    private String description;
    private String primaryDiseaseCode;
    private Set<String> secondaryDiseaseCodes;
    private Set<String> symptomCodes;
    private Set<String> currentMedicationCodes;
    private String requiredSpecialization;
    private Set<String> secondarySpecializations;
    private UrgencyLevel urgencyLevel;
    private CaseComplexity complexity;
    private Boolean requiresSecondOpinion;
    private Integer minDoctorsRequired;
    private Integer maxDoctorsAllowed;
    private List<Long> documentIds;
}