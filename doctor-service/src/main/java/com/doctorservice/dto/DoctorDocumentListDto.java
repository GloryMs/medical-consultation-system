package com.doctorservice.dto;

import lombok.*;

import java.util.List;

/**
 * DTO for listing all documents of a doctor
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDocumentListDto {
    private Long doctorId;
    private List<DoctorDocumentDto> documents;
    private Boolean hasAllRequiredDocuments;
    private Boolean allDocumentsVerified;
    private Boolean readyForVerification;
    private Integer totalDocuments;
    private Integer verifiedDocuments;
}

