package com.commonlibrary.dto;

import com.commonlibrary.entity.CaseComplexity;
import com.commonlibrary.entity.VerificationStatus;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Data
@Validated
@ToString
public class DoctorProfileDto {

    private Long id;
    private Long userId;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    @NotBlank(message = "Primary specialization is required")
    private String primarySpecialization;

    @Size(max = 5, message = "Maximum 5 specializations allowed")
    private Set<String> subSpecializations = new HashSet<>();

    @Size(max = 50, message = "Maximum 50 disease expertise areas allowed")
    private Set<String> diseaseExpertiseCodes = new HashSet<>();

    @Size(max = 100, message = "Maximum 100 symptom expertise areas allowed")
    private Set<String> symptomExpertiseCodes = new HashSet<>();

    @Min(value = 0, message = "Years of experience cannot be negative")
    @Max(value = 60, message = "Years of experience cannot exceed 60")
    private Integer yearsOfExperience;

    private Set<String> certifications = new HashSet<>();

    @Size(max = 1000, message = "Research areas cannot exceed 1000 characters")
    private String researchAreas;

    @Min(value = 1, message = "Must accept at least 1 concurrent case")
    @Max(value = 50, message = "Cannot exceed 50 concurrent cases")
    private Integer maxConcurrentCases = 10;

    private Boolean acceptsSecondOpinions = true;
    private Boolean acceptsComplexCases = true;

    private Set<String> preferredCaseTypes = new HashSet<>();
    private CaseComplexity maxComplexityLevel = CaseComplexity.HIGHLY_COMPLEX;
    private Boolean acceptsUrgentCases = true;

    private Integer consultationCount = 0;

    private Double rating = 0.0;
    private String professionalSummary;

    private VerificationStatus verificationStatus;
    private String phoneNumber;
    private String email;
    private String hospitalAffiliation;
    private String qualifications;
    private String languages;

    private Double caseRate;
    private Double hourlyRate;
    private Double emergencyRate;

    private String address;
    private String city;
    private String country;
}
