package com.commonlibrary.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Lightweight DTO for listing doctors in admin panel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorSummaryDto {
    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String licenseNumber;
    private String primarySpecialization;
    private Set<String> subSpecializations;
    
    private String verificationStatus;
    private Boolean isAvailable;
    private Boolean emergencyMode;
    
    private Integer yearsOfExperience;
    private Integer consultationCount;
    private Double rating;
    private Integer totalRatings;
    
    private Integer activeCases;
    private Double workloadPercentage;
    
    private String city;
    private String country;
    private String hospitalAffiliation;
    
    private LocalDateTime createdAt;
    private LocalDateTime verifiedAt;
}