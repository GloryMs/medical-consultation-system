package com.commonlibrary.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for rejecting a supervisor application (Admin only)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectSupervisorRequest {
    
    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;
}
