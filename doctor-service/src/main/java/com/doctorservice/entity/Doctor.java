package com.doctorservice.entity;

import com.commonlibrary.entity.BaseEntity;
import com.commonlibrary.entity.CaseComplexity;
import com.commonlibrary.entity.VerificationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Doctor extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String licenseNumber;

    // Enhanced Specialization Information
    @Column(nullable = false)
    private String primarySpecializationCode;

    @ElementCollection
    @CollectionTable(name = "doctor_specializations",
            joinColumns = @JoinColumn(name = "doctor_id"))
    @Column(name = "specialization_code")
    private Set<String> specializationCodes;

    @ElementCollection
    @CollectionTable(name = "doctor_subspecializations",
            joinColumns = @JoinColumn(name = "doctor_id"))
    @Column(name = "subspecialization_code")
    private Set<String> subSpecializationCodes;

    // Disease Expertise
    @ElementCollection
    @CollectionTable(name = "doctor_disease_expertise",
            joinColumns = @JoinColumn(name = "doctor_id"))
    @Column(name = "disease_code")
    private Set<String> diseaseExpertiseCodes;

    @ElementCollection
    @CollectionTable(name = "doctor_symptom_expertise",
            joinColumns = @JoinColumn(name = "doctor_id"))
    @Column(name = "symptom_code")
    private Set<String> symptomExpertiseCodes;

    // Experience and Qualifications
    private Integer yearsOfExperience;

    @ElementCollection
    @CollectionTable(name = "doctor_certifications",
            joinColumns = @JoinColumn(name = "doctor_id"))
    @Column(name = "certification")
    private Set<String> certifications;

    @Column(columnDefinition = "TEXT")
    private String researchAreas;

    // Capacity and Availability
    @Column(nullable = false)
    private Integer maxConcurrentCases = 10;

    @Column(nullable = false)
    private Integer currentCaseLoad = 0;

    @Column(nullable = false)
    private Boolean isAvailable = true;

    @Column(nullable = false)
    private Boolean acceptsSecondOpinions = true;

    @Column(nullable = false)
    private Boolean acceptsComplexCases = true;

    // Performance Metrics
    private Double averageRating = 0.0;
    private Integer totalConsultations = 0;
    private Integer acceptedCases = 0;
    private Integer rejectedCases = 0;
    private Double averageResponseTime = 0.0; // in hours

    // Matching Preferences
    @ElementCollection
    @CollectionTable(name = "doctor_preferred_case_types",
            joinColumns = @JoinColumn(name = "doctor_id"))
    @Column(name = "case_type")
    private Set<String> preferredCaseTypes;

    @Enumerated(EnumType.STRING)
    private CaseComplexity maxComplexityLevel;

    @Column(nullable = false)
    private Boolean acceptsUrgentCases = true;

    // Financial
    private BigDecimal baseConsultationFee;
    private BigDecimal urgentCaseFee;
    private BigDecimal complexCaseFee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus verificationStatus;

    private LocalDateTime verifiedAt;
    private String verifiedBy;

    @Size(max=13)
    private String phoneNumber;

    @Size(max=50)
    @Email
    private String email;
}
