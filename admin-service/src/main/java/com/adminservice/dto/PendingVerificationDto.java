package com.adminservice.dto;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class PendingVerificationDto {
    private Long doctorId;
    private String fullName;
    private String licenseNumber;
    private String specialization;
    private LocalDateTime submittedAt;
    private String documentsUrl;
}