package com.doctorservice.dto;

import com.commonlibrary.entity.TimeSlot;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateAvailabilityDto {
    
    @NotNull(message = "Availability status is required")
    private Boolean isAvailable;
    
    @Valid
    private Set<TimeSlot> timeSlots;
    
    private String reason; // Optional reason for availability change
    
    private Boolean emergencyMode = false;
    
    private String emergencyReason;
}