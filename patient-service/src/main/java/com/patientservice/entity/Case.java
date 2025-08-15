package com.patientservice.entity;

import com.commonlibrary.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "cases")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Case extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @OneToMany(mappedBy = "medicalCase",fetch = FetchType.EAGER)
    List<Document> documents;

    private Long assignedDoctorId;

    @Column(nullable = false)
    private String caseTitle;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    private String category;

    private String subCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UrgencyLevel urgencyLevel;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private BigDecimal consultationFee;

    private LocalDateTime acceptedAt;

    private LocalDateTime scheduledAt;

    private LocalDateTime paymentCompletedAt;

    private LocalDateTime closedAt;

    private String rejectionReason;

    private Integer rejectionCount = 0;
}