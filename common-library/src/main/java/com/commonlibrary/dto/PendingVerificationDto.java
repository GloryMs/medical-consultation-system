package com.commonlibrary.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PendingVerificationDto {
    private Long doctorId;
    private String fullName;
    private String licenseNumber;
    private String specialization;
    private LocalDateTime submittedAt;
    private String documentsUrl;
}