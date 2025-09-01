package com.patientservice.dto;

import com.commonlibrary.entity.CaseStatus;
import com.commonlibrary.entity.PaymentStatus;
import com.commonlibrary.entity.UrgencyLevel;
import com.patientservice.entity.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    private BigDecimal consultationFee;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime scheduledAt;
    private LocalDateTime paymentCompletedAt;
    private LocalDateTime closedAt;
    private String rejectionReason;
    private List<Document> documents;
}
