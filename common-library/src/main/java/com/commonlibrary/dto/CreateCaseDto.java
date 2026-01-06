package com.commonlibrary.dto;

import com.commonlibrary.entity.CaseComplexity;
import com.commonlibrary.entity.UrgencyLevel;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
@Validated
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class CreateCaseDto {

    // NEW: Optional dependent ID - if null, case is for the patient themselves
    private Long dependentId;

    @NotBlank(message = "Case title is required")
    @Size(min = 10, max = 100, message = "Case title must be between 10 and 100 characters")
    private String caseTitle;

    @NotBlank(message = "Description is required")
    @Size(min = 50, max = 2000, message = "Description must be between 50 and 2000 characters")
    private String description;

    @NotBlank(message = "Primary disease is required")
    private String primaryDiseaseCode;

    private Set<String> secondaryDiseaseCodes;

    private Set<String> symptomCodes;

    private Set<String> currentMedicationCodes;

    @NotBlank(message = "Required specialization is required")
    private String requiredSpecialization;

    private Set<String> secondarySpecializations;

    @NotNull(message = "Urgency level is required")
    private UrgencyLevel urgencyLevel;

    private CaseComplexity complexity;

    private Boolean requiresSecondOpinion = true;

    private Integer minDoctorsRequired = 2;

    private Integer maxDoctorsAllowed = 3;

    private List<Long> documentIds;

    // New field for file uploads
    @Size(max = 10, message = "Maximum 10 files allowed per case")
    private List<MultipartFile> files = new ArrayList<>();

    //For Medical Supervisor
    private Long supervisorId;


}