package com.patientservice.entity;

import com.commonlibrary.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reschedule_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleRequest extends BaseEntity {

    @Column(nullable = false)
    private Long caseId;

    @Column(nullable = false)
    private String requestedBy; // PATIENT or DOCTOR

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String preferredTimes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RescheduleStatus status;
}
