package com.commonlibrary.dto;

import com.commonlibrary.entity.VerificationStatus;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorFilterDto {
    private String searchTerm;              // Search in name, license, email
    private VerificationStatus verificationStatus;  // PENDING, VERIFIED, REJECTED
    private String specialization;          // Filter by primary specialization
    private Boolean isAvailable;            // true/false/null (all)
    private Boolean emergencyMode;          // true/false/null (all)
    private Integer minYearsExperience;
    private Integer maxYearsExperience;
    private Double minRating;
    private String city;
    private String country;
    
    // Pagination
    private Integer page = 0;
    private Integer size = 20;
    
    // Sorting
    private String sortBy = "createdAt";    // fullName, rating, createdAt, verifiedAt
    private String sortDirection = "DESC";   // ASC, DESC
}