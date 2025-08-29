package com.commonlibrary.dto;

import com.commonlibrary.entity.CaseComplexity;
import com.commonlibrary.entity.VerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Getter
public class DoctorDto {

    private Long doctorId;
    private String fullName;
    private String primarySpecializationCode;
    private Set<String> specializationCodes;
    private Set<String> subSpecializationCodes;
    private Set<String> diseaseExpertiseCodes;
    private Set<String> symptomExpertiseCodes;
    private String licenseNumber;
    private Integer yearsOfExperience;
    private Set<String> certifications;
    private String researchAreas;
    private Integer maxConcurrentCases;
    private Integer currentCaseLoad;
    private Boolean acceptsSecondOpinions;
    private Boolean acceptsComplexCases;
    private Double averageRating;
    private Integer totalConsultations;
    private Integer acceptedCases;
    private Integer rejectedCases;
    private Double averageResponseTime;
    private Set<String> preferredCaseTypes;
    private CaseComplexity maxComplexityLevel;
    private Boolean acceptsUrgentCases;
    private BigDecimal baseConsultationFee;
    private BigDecimal urgentCaseFee;
    private BigDecimal complexCaseFee;
    private Boolean isAvailable;
    private VerificationStatus verificationStatus;
    private LocalDateTime verifiedAt;
    private String verifiedBy;
    private String phoneNumber;
    private String email;
    private LocalDateTime createdAt;
}