package com.supervisorservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO for accepting an appointment on behalf of a patient
 */
@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptAppointmentDto {

    @NotNull(message = "Case ID is required")
    private Long caseId;

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    private String notes;
}