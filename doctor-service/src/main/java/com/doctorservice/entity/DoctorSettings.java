package com.doctorservice.entity;

import com.commonlibrary.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "doctor_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorSettings extends BaseEntity {

    @Column(nullable = false, unique = true)
    private Long doctorId;

    // Notification Preferences
    @Column(nullable = false)
    private Boolean emailNotifications = true;

    @Column(nullable = false)
    private Boolean smsNotifications = true;

    @Column(nullable = false)
    private Boolean pushNotifications = true;

    @Column(nullable = false)
    private Boolean newCaseAssignmentNotification = true;

    @Column(nullable = false)
    private Boolean appointmentRemindersNotification = true;

    @Column(nullable = false)
    private Boolean patientMessagesNotification = true;

    @Column(nullable = false)
    private Boolean systemUpdatesNotification = true;

    @Column(nullable = false)
    private Boolean promotionsNotification = false;

    // Availability Preferences
    @Column(nullable = false)
    private Boolean autoAcceptCases = false;

    @Column(nullable = false)
    private Integer maxDailyCases = 10;

    @Column(nullable = false)
    private Boolean allowEmergencyCases = true;

    @Column(nullable = false)
    private Boolean requiresConsultationFee = true;

    // Privacy Preferences
    @Column(nullable = false)
    private String profileVisibility = "verified_patients"; // private, verified_patients, all_patients, public

    @Column(nullable = false)
    private Boolean showRating = true;

    @Column(nullable = false)
    private Boolean showExperience = true;

    @Column(nullable = false)
    private Boolean allowReviews = true;

    // Accessibility Preferences
    @Column(nullable = false)
    private String theme = "light"; // light, dark, system

    @Column(nullable = false)
    private String fontSize = "medium"; // small, medium, large

    @Column(nullable = false)
    private String language = "en"; // en, de, fr, es

    @Column(nullable = false)
    private String timezone = "UTC";
}