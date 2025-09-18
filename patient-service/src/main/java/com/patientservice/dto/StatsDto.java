package com.patientservice.dto;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatsDto {
    Long totalCases;
    Long activeCases;
    Long completedCases;
    Long upcomingAppointments;
}
