package com.doctorservice.entity;

import com.commonlibrary.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appointment extends BaseEntity {

    @Column(nullable = false)
    private Long caseId;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private LocalDateTime scheduledTime;

    private Integer duration; // in minutes

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConsultationType consultationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    private Integer rescheduleCount = 0;

    private String meetingLink;

    private String meetingId;

    private LocalDateTime rescheduledFrom;

    private String rescheduleReason;

    private LocalDateTime completedAt;
}
