package com.supervisorservice.entity;

import jakarta.persistence.*;
import com.commonlibrary.entity.BaseEntity;
import lombok.*;

/**
 * Entity representing supervisor notification and preference settings
 */
@Entity
@Table(name = "supervisor_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class SupervisorSettings extends BaseEntity {
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id", nullable = false, unique = true)
    private MedicalSupervisor supervisor;
    
    // Notification Channels
    @Column(name = "email_notifications", nullable = false)
    @Builder.Default
    private Boolean emailNotifications = true;
    
    @Column(name = "sms_notifications", nullable = false)
    @Builder.Default
    private Boolean smsNotifications = false;
    
    @Column(name = "push_notifications", nullable = false)
    @Builder.Default
    private Boolean pushNotifications = true;
    
    // Notification Types
    @Column(name = "new_case_assignment_notification", nullable = false)
    @Builder.Default
    private Boolean newCaseAssignmentNotification = true;
    
    @Column(name = "appointment_reminders_notification", nullable = false)
    @Builder.Default
    private Boolean appointmentRemindersNotification = true;
    
    @Column(name = "case_status_update_notification", nullable = false)
    @Builder.Default
    private Boolean caseStatusUpdateNotification = true;
    
    @Column(name = "coupon_issued_notification", nullable = false)
    @Builder.Default
    private Boolean couponIssuedNotification = true;
    
    @Column(name = "coupon_expiring_notification", nullable = false)
    @Builder.Default
    private Boolean couponExpiringNotification = true;
    
    // Preferences
    @Column(name = "preferred_language", nullable = false, length = 10)
    @Builder.Default
    private String preferredLanguage = "EN";
    
    @Column(name = "timezone", nullable = false, length = 50)
    @Builder.Default
    private String timezone = "UTC";
    
    @Column(name = "theme", nullable = false, length = 20)
    @Builder.Default
    private String theme = "light";
}
