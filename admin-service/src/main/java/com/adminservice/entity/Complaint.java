package com.adminservice.entity;

import com.commonlibrary.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Complaint extends BaseEntity {
    
    @Column(nullable = false)
    private Long patientId;
    
    private Long doctorId;
    
    private Long caseId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintType complaintType;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintStatus status;
    
    @Column(columnDefinition = "TEXT")
    private String adminResponse;
    
    private Long assignedTo;
    
    private LocalDateTime resolvedAt;
    
    @Enumerated(EnumType.STRING)
    private ComplaintPriority priority;
}