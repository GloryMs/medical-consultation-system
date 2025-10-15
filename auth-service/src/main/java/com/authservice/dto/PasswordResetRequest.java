package com.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordResetRequest {
    @NotBlank(message = "Email or phone number is required")
    private String identifier; // Can be email or phone number
}