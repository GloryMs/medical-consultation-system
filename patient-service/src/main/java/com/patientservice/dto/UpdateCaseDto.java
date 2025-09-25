package com.patientservice.dto;

import com.commonlibrary.entity.CaseComplexity;
import com.commonlibrary.entity.UrgencyLevel;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;
import java.util.Set;

@Data
@Validated
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCaseDto {
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
}
