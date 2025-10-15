package com.commonlibrary.dto;

import com.commonlibrary.entity.Gender;
import com.commonlibrary.entity.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomPatientDto {
    // Patient (actual case owner) information
    private Long patientId;
    private Long userId;
    private String fullName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String medicalHistory;
    private SubscriptionStatus subscriptionStatus;
    private String bloodGroup;
    private String allergies;
    private String chronicConditions;
    private String phoneNumber;
    private String email;
    private String address;
    private String city;
    private String country;
    private String postalCode;

    // NEW: Emergency contact (account owner if case is for dependent)
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactEmail;
    private String emergencyContactRelationship;

    // NEW: Dependent information flag
    private Boolean isForDependent;
    private String dependentRelationship; // e.g., "SON", "DAUGHTER", "WIFE"
}