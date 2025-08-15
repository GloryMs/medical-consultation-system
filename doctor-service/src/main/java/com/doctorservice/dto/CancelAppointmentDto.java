package com.doctorservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CancelAppointmentDto {
    @NotBlank(message = "Reason is required")
    private String reason;
}