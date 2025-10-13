package com.commonlibrary.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorSettingsDto {
    
    // Nested DTOs for organized structure
    private NotificationPreferences notifications;
    private AvailabilityPreferences availability;
    private PrivacyPreferences privacy;
    private AccessibilityPreferences accessibility;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationPreferences {
        private Boolean email;
        private Boolean sms;
        private Boolean push;
        private Boolean newCaseAssignment;
        private Boolean appointmentReminders;
        private Boolean patientMessages;
        private Boolean systemUpdates;
        private Boolean promotions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailabilityPreferences {
        private Boolean autoAcceptCases;
        private Integer maxDailyCases;
        private Boolean allowEmergencyCases;
        private Boolean requiresConsultationFee;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrivacyPreferences {
        private String profileVisibility;
        private Boolean showRating;
        private Boolean showExperience;
        private Boolean allowReviews;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccessibilityPreferences {
        private String theme;
        private String fontSize;
        private String language;
        private String timezone;
    }
}