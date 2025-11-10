package com.commonlibrary.dto;

import com.commonlibrary.entity.AppointmentStatus;
import com.commonlibrary.entity.BaseEntity;
import com.commonlibrary.entity.ConsultationType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDto{

    private Long id;
    private Long caseId;
    private DoctorDto doctor;
    private Long patientId;
    private String patientName;
    private BigDecimal consultationFee;
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
