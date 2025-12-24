package com.commonlibrary.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorVerificationResponseDto {
    private Long doctorId;
    private String fullName;
    private String doctorEmail;
    private String status; // VERIFIED, REJECTED
    private String message;
    private LocalDateTime processedAt;
    private String processedBy; // Admin username
}