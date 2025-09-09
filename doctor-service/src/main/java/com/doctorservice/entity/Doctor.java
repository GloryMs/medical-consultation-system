// Updated Doctor.java entity
package com.doctorservice.entity;

import com.commonlibrary.entity.BaseEntity;
import com.commonlibrary.entity.TimeSlot;
import com.commonlibrary.entity.VerificationStatus;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Doctor extends BaseEntity {

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String licenseNumber;

    // ===== SPECIALIZATION FIELDS =====
    @Column(nullable = false)
    private String primarySpecialization;

    @ElementCollection
    @CollectionTable(name = "doctor_sub_specializations",
            joinColumns = @JoinColumn(name = "doctor_id"))
    @Column(name = "specialization")
    private Set<String> subSpecializations = new HashSet<>();

    // ===== PRICING FIELDS =====

    private Double hourlyRate;
    private Double caseRate;
    private Double emergencyRate;

    // ===== VERIFICATION & STATUS =====
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(nullable = false)
    private Boolean isAvailable = true;

    @Column(nullable = false)
    private Boolean emergencyMode = false;

    private String emergencyModeReason;

    private LocalDateTime emergencyModeEnabledAt;

    private LocalDateTime verifiedAt;

    // ===== PERFORMANCE METRICS =====
    @Column(nullable = false)
    private Integer consultationCount = 0;

    //@Column(precision = 3, scale = 2)
    private Double rating = 0.0;

    private Integer totalRatings = 0;

    //@Column(precision = 5, scale = 2)
    private Double completionRate = 100.0; // Percentage of completed cases

    // ===== WORKLOAD MANAGEMENT =====
    @Column(nullable = false)
    private Integer activeCases = 0;

    @Column(nullable = false)
    private Integer todayAppointments = 0;

    //@Column(precision = 5, scale = 2)
    private Double workloadPercentage = 0.0;

    private LocalDateTime lastWorkloadUpdate;

    @Column(nullable = false)
    private Integer maxActiveCases = 10;

    @Column(nullable = false)
    private Integer maxDailyAppointments = 8;

    // ===== AVAILABILITY SCHEDULE =====
    @ElementCollection
    @CollectionTable(name = "doctor_available_time_slots",
            joinColumns = @JoinColumn(name = "doctor_id"))
    private Set<TimeSlot> availableTimeSlots = new HashSet<>();

    // ===== PROFESSIONAL INFORMATION =====
    @Column(columnDefinition = "TEXT")
    private String professionalSummary;

    private Integer yearsOfExperience;

    @ElementCollection
    @CollectionTable(name = "doctor_qualifications",
            joinColumns = @JoinColumn(name = "doctor_id"))
    @Column(name = "qualification")
    private Set<String> qualifications = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "doctor_languages",
            joinColumns = @JoinColumn(name = "doctor_id"))
    @Column(name = "language")
    private Set<String> languages = new HashSet<>();

    // ===== CONTACT INFORMATION =====
    private String phoneNumber;

    private String email;

    private String hospitalAffiliation;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String city;

    private String country;

    // ===== RELATIONSHIPS =====
    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Appointment> appointments;

    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<ConsultationReport> consultationReports;

    // ===== BUSINESS LOGIC METHODS =====

    /**
     * Check if doctor can accept new cases
     */
    public boolean canAcceptNewCase() {
        return isAvailable &&
                (emergencyMode || activeCases < maxActiveCases) &&
                verificationStatus == VerificationStatus.VERIFIED;
    }

    /**
     * Check if doctor can schedule appointments today
     */
    public boolean canScheduleToday() {
        return isAvailable &&
                (emergencyMode || todayAppointments < maxDailyAppointments) &&
                verificationStatus == VerificationStatus.VERIFIED;
    }

    /**
     * Update consultation metrics
     */
    public void updateConsultationMetrics() {
        this.consultationCount++;
        this.lastWorkloadUpdate = LocalDateTime.now();
    }

    /**
     * Update rating with new rating value
     */
    public void updateRating(Double newRating) {
        if (newRating < 0 || newRating > 5) {
            throw new IllegalArgumentException("Rating must be between 0 and 5");
        }

        double totalScore = this.rating * this.totalRatings;
        this.totalRatings++;
        this.rating = (totalScore + newRating) / this.totalRatings;
    }

    /**
     * Calculate current workload percentage
     */
    public Double calculateCurrentWorkloadPercentage() {
        double caseLoad = (double) activeCases / maxActiveCases * 50;
        double appointmentLoad = (double) todayAppointments / maxDailyAppointments * 50;
        return Math.min(100.0, caseLoad + appointmentLoad);
    }

    /**
     * Check if doctor is overloaded
     */
    public boolean isOverloaded() {
        return !emergencyMode &&
                (activeCases >= maxActiveCases || todayAppointments >= maxDailyAppointments);
    }

    /**
     * Get available time slots for a specific day
     */
    public Set<TimeSlot> getAvailableTimeSlotsForDay(java.time.DayOfWeek dayOfWeek) {
        return availableTimeSlots.stream()
                .filter(slot -> slot.getDayOfWeek().equals(dayOfWeek) && slot.getIsAvailable())
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Add a new time slot with overlap validation
     */
    public void addTimeSlot(TimeSlot newSlot) {
        // Check for overlaps
        boolean hasOverlap = availableTimeSlots.stream()
                .anyMatch(existingSlot -> existingSlot.overlaps(newSlot));

        if (hasOverlap) {
            throw new IllegalArgumentException("Time slot overlaps with existing slot");
        }

        availableTimeSlots.add(newSlot);
    }

    /**
     * Remove time slots for a specific day
     */
    public void clearTimeSlotsForDay(java.time.DayOfWeek dayOfWeek) {
        availableTimeSlots.removeIf(slot -> slot.getDayOfWeek().equals(dayOfWeek));
    }

    /**
     * Enable emergency mode
     */
    public void enableEmergencyMode(String reason) {
        this.emergencyMode = true;
        this.emergencyModeReason = reason;
        this.emergencyModeEnabledAt = LocalDateTime.now();
        this.isAvailable = true; // Force availability in emergency mode
    }

    /**
     * Disable emergency mode
     */
    public void disableEmergencyMode() {
        this.emergencyMode = false;
        this.emergencyModeReason = null;
        this.emergencyModeEnabledAt = null;
    }

    /**
     * Get total weekly working hours
     */
    public long getTotalWeeklyWorkingHours() {
        return availableTimeSlots.stream()
                .mapToLong(TimeSlot::getDurationMinutes)
                .sum() / 60; // Convert to hours
    }

    /**
     * Check if doctor has capacity for urgent cases
     */
    public boolean hasUrgentCapacity() {
        return emergencyMode || workloadPercentage < 80.0;
    }
}