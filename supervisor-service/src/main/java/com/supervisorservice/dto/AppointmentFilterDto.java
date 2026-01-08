package com.supervisorservice.dto;

import com.commonlibrary.entity.AppointmentStatus;
import lombok.*;

import java.time.LocalDate;

/**
 * DTO for filtering appointments
 */
@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentFilterDto {

    private Long patientId;
    private Long caseId;
    private AppointmentStatus status;
    private LocalDate date;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean upcomingOnly;
    private String sortBy;
    private String sortOrder;
}