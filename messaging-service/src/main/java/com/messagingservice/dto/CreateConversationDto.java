package com.messagingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationDto {
    
    @NotNull(message = "Patient ID is required")
    private Long patientId;
    
    @NotNull(message = "Doctor ID is required")
    private Long doctorId;
    
    @NotNull(message = "Case ID is required")
    private Long caseId;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String patientName;
    private String doctorName;
}