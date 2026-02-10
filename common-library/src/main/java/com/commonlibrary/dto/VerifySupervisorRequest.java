package com.commonlibrary.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for verifying a supervisor (Admin only)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifySupervisorRequest {
    
    @NotBlank(message = "Verification notes are required")
    private String verificationNotes;
}
