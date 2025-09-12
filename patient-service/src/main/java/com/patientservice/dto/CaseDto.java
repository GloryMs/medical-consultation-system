package com.patientservice.dto;

import com.commonlibrary.entity.CaseComplexity;
import com.commonlibrary.entity.CaseStatus;
import com.commonlibrary.entity.PaymentStatus;
import com.commonlibrary.entity.UrgencyLevel;
import com.patientservice.entity.CaseAssignment;
import com.patientservice.entity.Document;
import com.patientservice.entity.Patient;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
public class CaseDto {
    private Long id;
    private String caseTitle;
    private String description;
    private CaseStatus status;
    private String requiredSpecialization;
    private LocalDateTime createdAt;
    private Patient patient;
    private List<Document> documents;
    private List<CaseAssignment> assignments = new ArrayList<>();
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
}