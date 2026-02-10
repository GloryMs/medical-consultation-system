package com.doctorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for submitting documents for review
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitDocumentsRequestDto {
    private String additionalNotes;
}
