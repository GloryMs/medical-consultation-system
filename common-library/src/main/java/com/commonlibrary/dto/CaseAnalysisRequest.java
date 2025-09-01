package com.commonlibrary.dto;

import lombok.Data;
import java.util.List;
import java.util.Set;

@Data
public class CaseAnalysisRequest {
    private String primaryDiseaseCode;
    private Set<String> secondaryDiseaseCodes;
    private List<String> symptomCodes;
    private String requiredSpecialization;
    private Set<String> secondarySpecializations;
    private String urgencyLevel;
    private String complexity;
}