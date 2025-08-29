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

    @NotBlank(message = "Case title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String caseTitle;

    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotBlank(message = "Primary disease is required")
    private String primaryDiseaseCode;

    @Size(max = 5, message = "Maximum 5 secondary diseases allowed")
    private Set<String> secondaryDiseaseCodes = new HashSet<>();

    @NotEmpty(message = "At least one symptom is required")
    @Size(max = 20, message = "Maximum 20 symptoms allowed")
    private Set<String> symptomCodes;

    @Size(max = 15, message = "Maximum 15 current medications allowed")
    private Set<String> currentMedicationCodes = new HashSet<>();

    @NotBlank(message = "Required specialization is required")
    private String requiredSpecialization;

    @Size(max = 3, message = "Maximum 3 secondary specializations allowed")
    private Set<String> secondarySpecializations = new HashSet<>();

    @NotNull(message = "Urgency level is required")
    private UrgencyLevel urgencyLevel;

    @NotNull(message = "Case complexity is required")
    private CaseComplexity complexity;

    private Boolean requiresSecondOpinion = true;

    @Min(value = 1, message = "Minimum 1 doctor required")
    @Max(value = 5, message = "Maximum 5 doctors allowed")
    private Integer minDoctorsRequired = 2;

    @Min(value = 1, message = "Minimum 1 doctor required")
    @Max(value = 5, message = "Maximum 5 doctors allowed")
    private Integer maxDoctorsAllowed = 3;

    private List<Long> documentIds = new ArrayList<>();
}