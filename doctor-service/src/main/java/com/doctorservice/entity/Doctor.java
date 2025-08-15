package com.doctorservice.entity;

import com.commonlibrary.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @Column(nullable = false)
    private String primarySpecialization;

    private String subSpecialization;

    private BigDecimal hourlyRate;

    private BigDecimal caseRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus verificationStatus;

    private Double averageRating = 0.0;

    private Integer consultationCount = 0;

    @Column(columnDefinition = "TEXT")
    private String professionalSummary;

    private Integer yearsOfExperience;

    @Column(columnDefinition = "TEXT")
    private String availableTimeSlots;

    @Column(nullable = false)
    private Boolean isAvailable = true;

    private LocalDateTime verifiedAt;

    private String verifiedBy;

    private String phoneNumber;

    private String email;

    private String hospitalAffiliation;

    private String qualifications;

    private String languages;
}
