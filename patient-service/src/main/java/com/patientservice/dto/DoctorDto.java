package com.patientservice.dto;
import com.commonlibrary.entity.CaseComplexity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Set;

@Data
public class DoctorDto {

    private Long doctorId;
    private String fullName;
    private String primarySpecializationCode;
    private Set<String> specializationCodes;
    private Set<String> subSpecializationCodes;
    private Set<String> diseaseExpertiseCodes;
    private Set<String> symptomExpertiseCodes;
    private Integer yearsOfExperience;
    private Set<String> certifications;
    private String researchAreas;
    private Integer maxConcurrentCases = 10;
    private Integer currentCaseLoad = 0;
    private Boolean acceptsSecondOpinions = true;
    private Boolean acceptsComplexCases = true;
    private Double averageRating = 0.0;
    private Integer totalConsultations = 0;
    private Integer acceptedCases = 0;
    private Integer rejectedCases = 0;
    private Double averageResponseTime = 0.0;
    private Set<String> preferredCaseTypes;
    private CaseComplexity maxComplexityLevel;
    private Boolean acceptsUrgentCases = true;
    private BigDecimal baseConsultationFee;
    private BigDecimal urgentCaseFee;
    private BigDecimal complexCaseFee;
    private Boolean isAvailable = true;
}
