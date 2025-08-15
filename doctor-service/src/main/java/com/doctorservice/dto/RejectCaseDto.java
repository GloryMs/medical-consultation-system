package com.doctorservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectCaseDto {
    @NotBlank(message = "Reason is required")
    private String reason;
}
