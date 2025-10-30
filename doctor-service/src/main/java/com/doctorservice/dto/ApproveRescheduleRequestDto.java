package com.doctorservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for doctor approving a re-schedule request
 * Sent when doctor selects one of patient's proposed times
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveRescheduleRequestDto {

    /**
     * Re-schedule ID
     */
    @NotNull(message = "Re-schedule ID is required")
    private Long rescheduleId;

    /**
     * The appointment ID to reschedule
     */
    @NotNull(message = "Appointment ID is required")
    private Long appointmentId;
    
    /**
     * Index of the selected preferred time from reschedule request
     */
    @NotNull(message = "Selected time required")
    private String newScheduledTime;
    
    /**
     * Optional reason or notes from doctor
     */
    private String reason;
}