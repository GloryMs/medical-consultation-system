package com.doctorservice.dto;

import com.doctorservice.entity.VerificationStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DoctorProfileDto {
    private Long id;
    private Long userId;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    @NotBlank(message = "Primary specialization is required")
    private String primarySpecialization;

    private String subSpecialization;
    private BigDecimal hourlyRate;
    private BigDecimal caseRate;
    private VerificationStatus verificationStatus;
    private String professionalSummary;
    private Integer yearsOfExperience;
    private String phoneNumber;
    private String email;
    private String hospitalAffiliation;
    private String qualifications;
    private String languages;
}
