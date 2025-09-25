package com.commonlibrary.dto;

import com.commonlibrary.entity.CaseComplexity;
import com.commonlibrary.entity.TimeSlot;
import com.commonlibrary.entity.VerificationStatus;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Getter
@Setter
public class DoctorDto {

    private Long id;
    private Long userId;
    private String fullName;
    private String licenseNumber;
    // ===== SPECIALIZATION FIELDS =====
    private String primarySpecialization;
    private Set<String> subSpecializations = new HashSet<>();
    // ===== PRICING FIELDS =====
    private BigDecimal hourlyRate;
    private BigDecimal caseRate;
    private BigDecimal emergencyRate;

    // ===== VERIFICATION & STATUS =====
    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;
    private Boolean isAvailable = true;
    private Boolean emergencyMode = false;
    private String emergencyModeReason;
    private LocalDateTime emergencyModeEnabledAt;
    private LocalDateTime verifiedAt;
    // ===== PERFORMANCE METRICS =====
    private Integer consultationCount = 0;
    private Double rating = 0.0;
    private Integer totalRatings = 0;
    private Double completionRate = 100.0; // Percentage of completed cases

    // ===== WORKLOAD MANAGEMENT =====
    private Integer activeCases = 0;
    private Integer todayAppointments = 0;
    private Double workloadPercentage = 0.0;
    private LocalDateTime lastWorkloadUpdate;
    private Integer maxActiveCases;
    private Integer maxDailyAppointments;

    // ===== AVAILABILITY SCHEDULE =====
    private Set<TimeSlot> availableTimeSlots = new HashSet<>();
    // ===== PROFESSIONAL INFORMATION =====
    private String professionalSummary;
    private Integer yearsOfExperience;
    private Set<String> qualifications = new HashSet<>();
    private Set<String> languages = new HashSet<>();
    // ===== CONTACT INFORMATION =====
    private String phoneNumber;
    private String email;
    private String hospitalAffiliation;
    private String address;
    private String city;
    private String country;

}