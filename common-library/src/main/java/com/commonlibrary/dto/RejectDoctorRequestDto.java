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
public class RejectDoctorRequestDto {
    @NotBlank(message = "Rejection reason is required")
    private String reason;
    
    private String additionalNotes;
}