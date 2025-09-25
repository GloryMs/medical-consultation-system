package com.patientservice.entity;

import com.commonlibrary.entity.*;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "cases")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Case extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @JsonBackReference
    private Patient patient;

    @OneToMany(mappedBy = "medicalCase", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Document> documents;

    @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL, orphanRemoval = true,fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<CaseAssignment> assignments = new ArrayList<>();

    @Column(nullable = false)
    private String caseTitle;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    // Structured Medical Information
    @Column(nullable = false)
    private String primaryDiseaseCode; // Main disease ICD code

    @ElementCollection
    @CollectionTable(name = "case_secondary_diseases",
            joinColumns = @JoinColumn(name = "case_id"))
    @Column(name = "disease_code")
    private Set<String> secondaryDiseaseCodes;

    @ElementCollection
    @CollectionTable(name = "case_symptoms",
            joinColumns = @JoinColumn(name = "case_id"))
    @Column(name = "symptom_code")
    private Set<String> symptomCodes;

    @ElementCollection
    @CollectionTable(name = "case_current_medications",
            joinColumns = @JoinColumn(name = "case_id"))
    @Column(name = "medication_code")
    private Set<String> currentMedicationCodes;

    @Column(nullable = false)
    private String requiredSpecialization; // Primary specialization needed

    @ElementCollection
    @CollectionTable(name = "case_secondary_specializations",
            joinColumns = @JoinColumn(name = "case_id"))
    @Column(name = "specialization")
    private Set<String> secondarySpecializations;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UrgencyLevel urgencyLevel;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    // Case characteristics for matching
    @Enumerated(EnumType.STRING)
    private CaseComplexity complexity;

    @Column(nullable = false)
    private Boolean requiresSecondOpinion = true;

    @Column(nullable = false)
    private Integer minDoctorsRequired = 2;

    @Column(nullable = false)
    private Integer maxDoctorsAllowed = 3;

    // Metadata
    private LocalDateTime submittedAt;
    private LocalDateTime firstAssignedAt;
    private LocalDateTime lastAssignedAt;
    private LocalDateTime closedAt;
    private Integer assignmentAttempts = 0;
    private Integer rejectionCount = 0;
}