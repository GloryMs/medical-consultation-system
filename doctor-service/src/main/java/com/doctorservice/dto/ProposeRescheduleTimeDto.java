package com.doctorservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO for doctor proposing a different reschedule time
 * Sent when doctor suggests an alternative time instead of patient's options
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProposeRescheduleTimeDto {
    
    /**
     * Doctor's proposed appointment time
     */
    @NotNull(message = "Proposed time is required")
    @Future(message = "Proposed time must be in the future")
    private LocalDateTime proposedTime;
    
    /**
     * Reason for proposing this specific time
     */
    @NotNull(message = "Reason is required")
    @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
    private String reason;
}