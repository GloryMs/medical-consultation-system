package com.commonlibrary.dto;

import com.commonlibrary.entity.SupervisorVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Medical Supervisor profile information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupervisorProfileDto {
    
    private Long id;
    private Long userId;
    private String fullName;
    private String organizationName;
    private String organizationType;
    private String licenseNumber;
    private String licenseDocumentPath;
    private String phoneNumber;
    private String email;
    private String address;
    private String city;
    private String country;
    private SupervisorVerificationStatus verificationStatus;
    private String verificationNotes;
    private LocalDateTime verifiedAt;
    private Long verifiedBy;
    private String rejectionReason;
    private Integer maxPatientsLimit;
    private Integer maxActiveCasesPerPatient;
    private Boolean isAvailable;
    private Integer activePatientCount;
    private Integer availableCouponCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
