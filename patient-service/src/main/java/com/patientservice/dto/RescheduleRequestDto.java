package com.patientservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RescheduleRequestDto {
    /**
     * Reason for requesting reschedule
     */
    @NotBlank(message = "Reason is required")
    @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
    private String reason;

    /**
     * List of preferred times in ISO 8601 format (yyyy-MM-ddThh:mm:ss)
     * At least one time must be provided
     */
    @NotEmpty(message = "At least one preferred time is required")
    @Size(min = 1, max = 5, message = "Provide 1-5 preferred times")
    private List<String> preferredTimes;

    /**
     * Optional additional notes or context
     */
    @Size(max = 500, message = "Additional notes cannot exceed 500 characters")
    private String additionalNotes;

    private Long appointmentId;
}
