package com.doctorservice.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO for Doctor Document information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDocumentDto {
    
    private Long id;
    private Long doctorId;
    private String documentType; // LICENSE, CERTIFICATE, EXPERIENCE
    private String fileName;
    private String fileUrl;
    private Double fileSizeKB; // Size in KB
    private String mimeType;
    private Boolean isEncrypted;
    private Boolean isCompressed;
    private String description;
    private LocalDateTime uploadedAt;
    private Boolean verifiedByAdmin;
    private LocalDateTime verifiedAt;
    private Long verifiedBy;
    private String verificationNotes;
    
    // URLs for frontend
    private String downloadUrl;
    private String viewUrl;
}