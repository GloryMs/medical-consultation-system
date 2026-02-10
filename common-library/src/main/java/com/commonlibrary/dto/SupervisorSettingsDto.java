package com.commonlibrary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for supervisor settings
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupervisorSettingsDto {
    
    private Long id;
    private Long supervisorId;
    
    // Notification Channels
    private Boolean emailNotifications;
    private Boolean smsNotifications;
    private Boolean pushNotifications;
    
    // Notification Types
    private Boolean newCaseAssignmentNotification;
    private Boolean appointmentRemindersNotification;
    private Boolean caseStatusUpdateNotification;
    private Boolean couponIssuedNotification;
    private Boolean couponExpiringNotification;
    
    // Preferences
    private String preferredLanguage;
    private String timezone;
    private String theme;
}
