package com.patientservice.dto;

import com.commonlibrary.entity.CaseStatus;
import com.commonlibrary.entity.PaymentStatus;
import com.commonlibrary.entity.UrgencyLevel;
import com.patientservice.entity.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
public class CaseDetailsDto {
    private Long id;
    private String caseTitle;
    private String description;
    private String category;
    private String subCategory;
    private CaseStatus status;
    private UrgencyLevel urgencyLevel;
    private Long assignedDoctorId;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime scheduledAt;
    private LocalDateTime paymentCompletedAt;
    private LocalDateTime closedAt;
    private String rejectionReason;
    private Boolean isDeleted = false;

    private String primaryDiseaseCode;
    private Set<String> secondaryDiseaseCodes;
    private Set<String> symptomCodes;
    private Set<String> currentMedicationCodes;
    private String requiredSpecialization;
    private Set<String> secondarySpecializations;
    private BigDecimal consultationFee;
    private LocalDateTime feeSetAt;

    // Enhanced document information
    private List<Document> documents;
    private Integer documentCount;
    private Long totalDocumentSize; // Total size of all documents

    // File access information
    private List<DocumentAccessDto> documentAccess;

    @Data
    public static class DocumentAccessDto {
        private Long documentId;
        private String fileName;
        private String mimeType;
        private Double fileSizeKB;
        private String documentType;
        private String accessUrl;
        private String downloadUrl;
        private Boolean isEncrypted;
        private Boolean isCompressed;
        private LocalDateTime uploadedAt;
        private String description;

    }
}
