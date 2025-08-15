package com.patientservice.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CaseDto {
    private Long id;
    private String caseTitle;
    private String description;
    private String category;
    private String status;
    private String urgencyLevel;
    private Long patientId;
    private LocalDateTime createdAt;
}