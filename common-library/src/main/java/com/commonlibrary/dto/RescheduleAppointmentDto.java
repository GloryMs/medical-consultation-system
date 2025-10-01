package com.commonlibrary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * DTO for rescheduling an appointment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleAppointmentDto {
    
    /**
     * New scheduled time for the appointment
     */
    @NotNull(message = "Scheduled time is required")
    @Future(message = "Scheduled time must be in the future")
    private LocalDateTime scheduledTime;
    
    /**
     * Reason for rescheduling (required for tracking and notification)
     */
    @NotNull(message = "Reason for rescheduling is required")
    @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
    private String reason;
    
    /**
     * Optional: New duration if changing from original
     */
    private Integer duration;
    
    /**
     * Optional: Additional notes for the rescheduled appointment
     */
    @Size(max = 1000, message = "Additional notes cannot exceed 1000 characters")
    private String additionalNotes;
}