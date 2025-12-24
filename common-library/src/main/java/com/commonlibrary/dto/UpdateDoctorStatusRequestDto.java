package com.commonlibrary.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDoctorStatusRequestDto {
    @NotBlank(message = "Status is required")
    private String status; // ACTIVE, INACTIVE, SUSPENDED
    
    private String reason;
}