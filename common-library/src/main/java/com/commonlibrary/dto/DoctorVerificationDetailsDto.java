package com.commonlibrary.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO for Doctor Verification Details
 * Used by Admin to review doctor applications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorVerificationDetailsDto {
    
    // Basic Information
    private Long doctorId;
    private Long userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String licenseNumber;
    
    // Specialization
    private String primarySpecialization;
    private Set<String> subSpecializations;
    
    // Professional Information
    private Integer yearsOfExperience;
    private String professionalSummary;
    private Set<String> qualifications;
    private Set<String> certifications;
    private Set<String> languages;
    
    // Location
    private String hospitalAffiliation;
    private String address;
    private String city;
    private String country;
    
    // Expertise
    private Set<String> diseaseExpertiseCodes;
    private Set<String> symptomExpertiseCodes;
    
    // Pricing
    private Double hourlyRate;
    private Double caseRate;
    private Double emergencyRate;
    
    // Verification Status
    private String verificationStatus;
    private LocalDateTime submittedAt;
    private LocalDateTime verifiedAt;
    
    // Documents (URLs or file paths)
    private String documentsUrl;
    private String medicalLicenseUrl;
    private String medicalDegreeUrl;
    private String certificationsUrl;
    private String identityDocumentUrl;
    
    // Statistics (if doctor was previously verified)
    private Integer consultationCount;
    private Double rating;
    private Integer totalRatings;
    
    // Additional verification info
    private String verificationNotes;
    private String rejectionReason;
}