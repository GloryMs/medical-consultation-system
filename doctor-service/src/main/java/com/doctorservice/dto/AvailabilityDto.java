package com.doctorservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AvailabilityDto {
    private String availableTimeSlots;

    @NotNull(message = "Availability status is required")
    private Boolean isAvailable;
}