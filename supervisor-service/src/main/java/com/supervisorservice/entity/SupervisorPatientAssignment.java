package com.supervisorservice.entity;

import com.commonlibrary.entity.BaseEntity;
import com.commonlibrary.entity.SupervisorAssignmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing the assignment of a patient to a supervisor
 */
@Entity
@Table(name = "supervisor_patient_assignments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class SupervisorPatientAssignment extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id", nullable = false)
    private MedicalSupervisor supervisor;
    
    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    private Long patientUserId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_status", nullable = false)
    @Builder.Default
    private SupervisorAssignmentStatus assignmentStatus = SupervisorAssignmentStatus.ACTIVE;
    
    @Column(name = "assigned_at", nullable = false)
    @Builder.Default
    private LocalDateTime assignedAt = LocalDateTime.now();
    
    @Column(name = "assigned_by")
    private Long assignedBy;
    
    @Column(name = "assignment_notes", columnDefinition = "TEXT")
    private String assignmentNotes;
    
    @Column(name = "terminated_at")
    private LocalDateTime terminatedAt;
    
    @Column(name = "termination_reason", columnDefinition = "TEXT")
    private String terminationReason;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    @Column(name = "removal_reason", columnDefinition = "TEXT")
    private String removalReason;
    
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    // Denormalized patient info for quick access
    @Column(name = "patient_name")
    private String patientName;

    @Column(name = "patient_email")
    private String patientEmail;

    @Column(name = "patient_phone")
    private String patientPhone;
    
    /**
     * Check if assignment is currently active
     */
    @Transient
    public boolean isActive() {
        return assignmentStatus == SupervisorAssignmentStatus.ACTIVE && !isDeleted;
    }
    
    /**
     * Terminate this assignment
     */
    public void terminate(String reason, Long terminatedBy) {
        this.assignmentStatus = SupervisorAssignmentStatus.TERMINATED;
        this.terminatedAt = LocalDateTime.now();
        this.terminationReason = reason;
    }


    public void remove(String reason) {
        this.assignmentStatus = SupervisorAssignmentStatus.REMOVED;
        this.removedAt = LocalDateTime.now();
        this.removalReason = reason;
    }
    /**
     * Suspend this assignment
     */
    public void suspend(String reason) {
        this.assignmentStatus = SupervisorAssignmentStatus.SUSPENDED;
        this.terminationReason = reason;
    }
    
    /**
     * Reactivate this assignment
     */
    public void reactivate() {
        this.assignmentStatus = SupervisorAssignmentStatus.ACTIVE;
        this.terminatedAt = null;
        this.terminationReason = null;
    }
}
