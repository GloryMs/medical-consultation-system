package com.commonlibrary.dto;

import com.commonlibrary.entity.CaseComplexity;
import com.commonlibrary.entity.CaseStatus;
import com.commonlibrary.entity.PaymentStatus;
import com.commonlibrary.entity.UrgencyLevel;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class CaseDto {
    private Long id;
    private Long patientId;
    private String patientName;
    private Long dependantId;
    private String caseTitle;
    private String description;
    private CaseStatus status;
    private String requiredSpecialization;
    private LocalDateTime createdAt;
    private String primaryDiseaseCode;
    private Set<String> secondaryDiseaseCodes;
    private Set<String> symptomCodes;
    private Set<String> currentMedicationCodes;
    private Set<String> secondarySpecializations;
    private PaymentStatus paymentStatus;
    private CaseComplexity complexity;
    private UrgencyLevel urgencyLevel;
    private Boolean requiresSecondOpinion;
    private Integer minDoctorsRequired = 2;
    private Integer maxDoctorsAllowed = 3;
    private LocalDateTime submittedAt;
    private LocalDateTime firstAssignedAt;
    private LocalDateTime lastAssignedAt;
    private LocalDateTime closedAt;
    private Integer assignmentAttempts = 0;
    private Integer rejectionCount = 0;
    private Boolean isDeleted = false;
    private BigDecimal consultationFee;
    private LocalDateTime feeSetAt;
    private String medicalReportFileLink;
    private Long reportId;

    //added for admin use for now: must be checked later to be added/updated from different places
    private String doctorName;
    private Long assignedDoctorId;
}