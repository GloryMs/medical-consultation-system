package com.patientservice.dto;

import com.commonlibrary.entity.AssignmentPriority;
import com.commonlibrary.entity.AssignmentStatus;
import com.commonlibrary.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CaseAssignmentDto extends BaseEntity {

    private Long caseId;
    private Long doctorId;
    private AssignmentStatus status;
    private AssignmentPriority priority; // PRIMARY, SECONDARY, CONSULTANT
    private LocalDateTime assignedAt;
    private LocalDateTime respondedAt;
    private LocalDateTime expiresAt;
    private String assignmentReason;
    private String rejectionReason;
    private Double matchingScore;
}