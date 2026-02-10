package com.doctorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for document verification by admin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVerificationDto {
    private Boolean verified;
    private String verificationNotes;
    private Long verifiedBy; // Admin user ID
}
