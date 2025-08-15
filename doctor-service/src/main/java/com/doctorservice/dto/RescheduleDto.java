package com.doctorservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RescheduleDto {
    @NotNull(message = "New scheduled time is required")
    private LocalDateTime scheduledTime;

    private String reason;
}
