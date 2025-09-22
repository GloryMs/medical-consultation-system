package com.patientservice.dto;

import com.patientservice.entity.Document;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseAttachmentsDto {
    
    private Long caseId;
    private String caseTitle;
    private Integer totalDocuments;
    private Integer newDocumentsUploaded;
    private Long totalSizeBytes;
    private Integer remainingSlots; // How many more files can be uploaded
    private LocalDateTime lastUploadTime;
    private List<DocumentSummaryDto> documents;
    private List<DocumentSummaryDto> newDocuments; // Only newly uploaded documents
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentSummaryDto {
        private Long id;
        private String fileName;
        private String mimeType;
        private Double fileSizeKB;
        private String documentType;
        private String accessUrl;
        private String downloadUrl;
        private Boolean isEncrypted;
        private Boolean isCompressed;
        private LocalDateTime uploadedAt;
        private String description;
        private Boolean isNewUpload; // Flag to indicate if this was just uploaded
    }
}