package com.doctorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for document submission response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitDocumentsResponseDto {
    private Boolean success;
    private String message;
    private Integer documentsSubmitted;
    private Boolean hasAllRequiredDocuments;
}
