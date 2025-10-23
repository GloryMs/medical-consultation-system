package com.doctorservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for doctor rejecting a reschedule request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectRescheduleRequestDto {
    
    /**
     * Reason for rejecting the reschedule request
     */
    @NotNull(message = "Rejection reason is required")
    @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
    private String rejectionReason;
}