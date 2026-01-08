package com.patientservice.entity;

import com.commonlibrary.entity.BaseEntity;
import com.commonlibrary.entity.RescheduleStatus;
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
    private Long patientId;

    @Column(nullable = false)
    private Long appointmentId;

    @Column(nullable = false)
    private String requestedBy; // PATIENT or Supervisor

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String preferredTimes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RescheduleStatus status;

    @Column(name = "requested_by_supervisor_id")
    private Long requestedBySupervisorId;

}
