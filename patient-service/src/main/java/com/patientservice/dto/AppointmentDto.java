package com.patientservice.dto;

import com.doctorservice.entity.AppointmentStatus;
import com.doctorservice.entity.ConsultationType;
import com.doctorservice.entity.Doctor;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDto {
    private Long caseId;
    private Long doctorId;
    private Long patientId;
    private String doctorName;
    private LocalDateTime scheduledTime;
    private Integer duration;
    private ConsultationType consultationType;
    private AppointmentStatus status;
    private Integer rescheduleCount;
    private String meetingLink;
    private String meetingId;
    private LocalDateTime rescheduledFrom;
    private String rescheduleReason;
    private LocalDateTime completedAt;
}
