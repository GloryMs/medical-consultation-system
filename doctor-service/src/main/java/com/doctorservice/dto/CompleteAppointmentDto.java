package com.doctorservice.dto;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompleteAppointmentDto {
    private Long caseId;
    private Long patientId;
    private Long appointmentId;
}
