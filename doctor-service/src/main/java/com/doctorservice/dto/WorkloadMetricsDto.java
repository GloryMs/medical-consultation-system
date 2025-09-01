package com.doctorservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkloadMetricsDto {
    private Integer activeCases;
    private Integer todayAppointments;
    private Integer thisWeekAppointments;
}