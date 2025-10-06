package com.doctorservice.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReportDto {

    @Size(max = 5000, message = "Diagnosis cannot exceed 5000 characters")
    private String diagnosis;

    @Size(max = 5000, message = "Recommendations cannot exceed 5000 characters")
    private String recommendations;

    @Size(max = 5000, message = "Prescriptions cannot exceed 5000 characters")
    private String prescriptions;

    @Size(max = 3000, message = "Follow-up instructions cannot exceed 3000 characters")
    private String followUpInstructions;

    @Size(max = 10000, message = "Doctor notes cannot exceed 10000 characters")
    private String doctorNotes;

    private Boolean requiresFollowUp;

    private LocalDateTime nextAppointmentSuggested;
}