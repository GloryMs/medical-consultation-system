package com.doctorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for document upload response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadResponseDto {
    private Long documentId;
    private String fileName;
    private String documentType;
    private Double fileSizeKB;
    private String message;
    private String fileUrl;
}
