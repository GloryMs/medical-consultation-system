package com.patientservice.entity;

import com.commonlibrary.entity.BaseEntity;
import com.commonlibrary.entity.Gender;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Represents a family member/dependent for whom the patient can submit cases
 */
@Entity
@Table(name = "dependents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dependent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @JsonBackReference
    private Patient patient;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String relationship; // SON, DAUGHTER, WIFE, HUSBAND, MOTHER, FATHER, OTHER

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(columnDefinition = "TEXT")
    private String medicalHistory;

    private String bloodGroup;

    @Column(columnDefinition = "TEXT")
    private String allergies;

    @Column(columnDefinition = "TEXT")
    private String chronicConditions;

    private String phoneNumber; // Optional: dependent's own phone

    @Column(columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean isDeleted = false;

    // Optional: Profile picture path
    private String profilePicture;
}