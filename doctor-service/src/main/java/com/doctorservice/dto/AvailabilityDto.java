package com.doctorservice.dto;

import com.commonlibrary.entity.TimeSlot;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class AvailabilityDto {

    @NotNull(message = "Availability status is required")
    private Boolean isAvailable;

    @Valid
    private Set<TimeSlot> availableTimeSlots = new HashSet<>();

    private String reason; // Optional reason for availability change

    private Boolean emergencyMode = false;

    private String emergencyReason;

    // Capacity settings
    private Integer maxActiveCases;

    private Integer maxDailyAppointments;

    // Quick availability toggle (without changing time slots)
    private Boolean quickToggle = false;
}