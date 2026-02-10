package com.supervisorservice.entity;

import com.commonlibrary.entity.BaseEntity;
import com.commonlibrary.entity.SupervisorVerificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a Medical Supervisor
 * Medical supervisors manage patients and submit cases on their behalf
 */
@Entity
@Table(name = "medical_supervisors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class MedicalSupervisor extends BaseEntity {
    
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    
    @Column(name = "full_name", nullable = false)
    private String fullName;
    
    @Column(name = "organization_name")
    private String organizationName;
    
    @Column(name = "organization_type", length = 100)
    private String organizationType;
    
    @Column(name = "license_number", length = 100)
    private String licenseNumber;
    
    @Column(name = "license_document_path", length = 500)
    private String licenseDocumentPath;
    
    @Column(name = "phone_number", length = 50)
    private String phoneNumber;
    
    @Column(name = "email", nullable = false)
    private String email;
    
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;
    
    @Column(name = "city", length = 100)
    private String city;
    
    @Column(name = "country", length = 100)
    private String country;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    @Builder.Default
    private SupervisorVerificationStatus verificationStatus = SupervisorVerificationStatus.PENDING;
    
    @Column(name = "verification_notes", columnDefinition = "TEXT")
    private String verificationNotes;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @Column(name = "verified_by")
    private Long verifiedBy;
    
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    
    @Column(name = "max_patients_limit", nullable = false)
    @Builder.Default
    private Integer maxPatientsLimit = 3;
    
    @Column(name = "max_active_cases_per_patient", nullable = false)
    @Builder.Default
    private Integer maxActiveCasesPerPatient = 2;
    
    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;
    
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;
    
    @OneToMany(mappedBy = "supervisor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SupervisorPatientAssignment> patientAssignments = new ArrayList<>();
    
    @OneToMany(mappedBy = "supervisor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SupervisorCoupon> coupons = new ArrayList<>();
    
    @OneToOne(mappedBy = "supervisor", cascade = CascadeType.ALL, orphanRemoval = true)
    private SupervisorSettings settings;


    
    /**
     * Get count of active patient assignments
     */
    @Transient
    public int getActivePatientCount() {
        if (patientAssignments == null) return 0;
        return (int) patientAssignments.stream()
                .filter(assignment -> assignment.getAssignmentStatus() == com.commonlibrary.entity.SupervisorAssignmentStatus.ACTIVE
                        && !assignment.getIsDeleted())
                .count();
    }
    
    /**
     * Get count of available coupons
     */
    @Transient
    public int getAvailableCouponCount() {
        if (coupons == null) return 0;
        return (int) coupons.stream()
                .filter(coupon -> coupon.getStatus() == com.commonlibrary.entity.CouponStatus.AVAILABLE
                        && !coupon.getIsDeleted()
                        && coupon.getExpiresAt().isAfter(LocalDateTime.now()))
                .count();
    }
    
    /**
     * Check if supervisor can add more patients
     */
    @Transient
    public boolean canAddPatient() {
        return getActivePatientCount() < maxPatientsLimit;
    }
    
    /**
     * Check if supervisor is verified and available
     */
    @Transient
    public boolean isActiveAndVerified() {
        return verificationStatus == SupervisorVerificationStatus.VERIFIED
                && isAvailable
                && !isDeleted;
    }
}
