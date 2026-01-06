package com.commonlibrary.dto;

import com.commonlibrary.entity.Gender;
import com.commonlibrary.entity.SubscriptionStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PatientProfileDto {
    private Long id;
    private Long userId;
    private String fullName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String medicalHistory;
    private SubscriptionStatus subscriptionStatus;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String bloodGroup;
    private String allergies;
    private String chronicConditions;
    private String phoneNumber;
    private String email;
    private String address;
    private String city;
    private String country;
    private String postalCode;
}