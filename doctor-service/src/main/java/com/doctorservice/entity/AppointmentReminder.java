package com.doctorservice.entity;

import com.commonlibrary.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "appointment_reminders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentReminder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Column(nullable = false)
    private Long recipientUserId; // Doctor or Patient userId
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RecipientType recipientType; // DOCTOR or PATIENT
    
    @Column(nullable = false)
    private Integer minutesBefore; // e.g., 60 for 1 hour, 1440 for 24 hours
    
    @Column(nullable = false)
    private LocalDateTime scheduledSendTime; // When to send (in UTC)
    
    @Column(nullable = false)
    private LocalDateTime recipientLocalTime; // Appointment time in recipient's timezone
    
    @Column(nullable = false)
    private String recipientTimezone; // e.g., "Europe/Berlin", "America/New_York"
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReminderStatus status; // PENDING, SENT, FAILED, CANCELLED
    
    private LocalDateTime sentAt;
    
    private String failureReason;
    
    @Column(nullable = false)
    private Boolean emailSent = false;
    
    @Column(nullable = false)
    private Boolean inAppNotificationSent = false;

    public enum RecipientType {
        DOCTOR, PATIENT
    }

}