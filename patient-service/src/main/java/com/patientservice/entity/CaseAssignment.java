package com.patientservice.entity;

import com.commonlibrary.entity.AssignmentPriority;
import com.commonlibrary.entity.AssignmentStatus;
import com.commonlibrary.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "case_assignments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseAssignment extends BaseEntity {
    
    @ManyToOne
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;
    
    @Column(nullable = false)
    private Long doctorId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentStatus status;
    
    @Enumerated(EnumType.STRING)
    private AssignmentPriority priority; // PRIMARY, SECONDARY, CONSULTANT
    
    private LocalDateTime assignedAt;
    private LocalDateTime respondedAt;
    private LocalDateTime expiresAt;
    
    @Column(columnDefinition = "TEXT")
    private String assignmentReason; // Why this doctor was selected
    
    @Column(columnDefinition = "TEXT")
    private String rejectionReason;
    
    private Double matchingScore; // Algorithm-calculated matching score
}