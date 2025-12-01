package com.patientservice.entity;

import com.commonlibrary.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity to track reminder notifications sent for case assignments
 * Prevents duplicate reminders and provides audit trail
 */
@Entity
@Table(name = "case_assignment_reminders", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"assignment_id", "reminder_hour"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseAssignmentReminder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private CaseAssignment assignment;

    /**
     * Hours from assignment when this reminder was scheduled (e.g., 12, 20, 23)
     */
    @Column(nullable = false)
    private Integer reminderHour;

    /**
     * When the reminder was actually sent
     */
    @Column(nullable = false)
    private LocalDateTime sentAt;

    /**
     * Hours remaining until expiration when reminder was sent
     */
    @Column(nullable = false)
    private Long hoursRemaining;

    /**
     * Additional notes about the reminder
     */
    @Column(columnDefinition = "TEXT")
    private String notes;
}