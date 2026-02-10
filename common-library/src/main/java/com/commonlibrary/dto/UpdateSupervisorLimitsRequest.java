package com.commonlibrary.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating supervisor limits (Admin only)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSupervisorLimitsRequest {
    
    @NotNull(message = "Max patients limit is required")
    @Min(value = 1, message = "Max patients limit must be at least 1")
    private Integer maxPatientsLimit;
    
    @NotNull(message = "Max active cases per patient is required")
    @Min(value = 1, message = "Max active cases per patient must be at least 1")
    private Integer maxActiveCasesPerPatient;
}
