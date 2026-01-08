package com.supervisorservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for creating a reschedule request on behalf of a patient
 */
@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupervisorRescheduleRequestDto {

    @NotNull(message = "Appointment ID is required")
    private Long appointmentId;

    @NotNull(message = "Case ID is required")
    private Long caseId;

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotEmpty(message = "At least one preferred time is required")
    @Size(min = 1, max = 5, message = "Provide 1-5 preferred times")
    private List<String> preferredTimes;

    @NotNull(message = "Reason is required")
    private String reason;

    private String additionalNotes;
}