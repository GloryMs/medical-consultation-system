package com.patientservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class RescheduleRequestDto {
    @NotBlank(message = "Reason is required")
    private String reason;

    @NotEmpty(message = "Preferred times are required")
    private List<String> preferredTimes;
}
