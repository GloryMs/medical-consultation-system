package com.patientservice.dto;

import com.commonlibrary.entity.CaseComplexity;
import com.commonlibrary.entity.UrgencyLevel;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Validated
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    // New field for file uploads
    @Size(max = 10, message = "Maximum 10 files allowed per case")
    private List<MultipartFile> files = new ArrayList<>();
}