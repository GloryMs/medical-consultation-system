package com.commonlibrary.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ComplaintDto {
    private Long patientId;
    private Long doctorId;
    private Long caseId;
    private String complaintType;
    private String description;
    private String priority;
    private String status;
    private String adminResponse;
    private Long assignedTo;
    private LocalDateTime resolvedAt;
}
