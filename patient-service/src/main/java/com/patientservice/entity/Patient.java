package com.patientservice.entity;

import com.commonlibrary.entity.BaseEntity;
import com.commonlibrary.entity.Gender;
import com.commonlibrary.entity.SubscriptionStatus;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patients")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String fullName;

    private LocalDate dateOfBirth;

    // NEW: List of dependents (family members)
    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Dependent> dependents = new ArrayList<>();

    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference
    private List<Case> medicalCase;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(columnDefinition = "TEXT")
    private String medicalHistory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus subscriptionStatus;

    private LocalDateTime subscriptionExpiry;

    private LocalDateTime subscriptionPaymentDate;

    private Integer casesSubmitted = 0;

    private String emergencyContactName;

    private String emergencyContactPhone;

    @Column(nullable = false)
    private Boolean accountLocked = false;

    private String bloodGroup;

    @Column(columnDefinition = "TEXT")
    private String allergies;

    @Column(columnDefinition = "TEXT")
    private String chronicConditions;

    private String phoneNumber;

    private String email;

    private String address;

    private String city;

    private String country;

    private String postalCode;
}