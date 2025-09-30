package com.patientservice.controller;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.exception.BusinessException;
import com.patientservice.entity.Document;
import com.patientservice.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final DocumentService documentService;

    /**
     * Serve file content directly (for viewing in browser)
     */
    @GetMapping("/{caseId}/{documentId}")
    public ResponseEntity<byte[]> serveFile(
            @PathVariable Long caseId,
            @PathVariable Long documentId,
            @RequestHeader("X-User-Id") Long userId) {
        
        try {
            byte[] fileContent = documentService.getFileContent(caseId, documentId, userId);

            Document document = documentService.getDocumentById( documentId );
            if( document == null ) {
                throw new BusinessException("Document not found", HttpStatus.NOT_FOUND);
            }

            // Set appropriate headers based on file type
            HttpHeaders headers = new HttpHeaders();
            
            if (document.getMimeType() != null) {
                headers.setContentType(MediaType.parseMediaType(document.getMimeType()));
            } else {
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            }
            
            // For PDF files, display inline; for images, also inline
            if (document.getMimeType() != null && 
                (document.getMimeType().equals("application/pdf") || 
                 document.getMimeType().startsWith("image/"))) {
                headers.setContentDispositionFormData("inline", document.getFileName());
            } else {
                headers.setContentDispositionFormData("attachment", document.getFileName());
            }
            
            headers.setContentLength(fileContent.length);
            headers.setCacheControl("max-age=3600"); // Cache for 1 hour

            log.info("Serving file: {} for user: {}", document.getFileName(), userId);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);
                    
        } catch (Exception e) {
            log.error("Error serving file {} for user {}: {}", documentId, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get file metadata without content
     */
    @GetMapping("/{documentId}/metadata")
    public ResponseEntity<ApiResponse<Object>> getFileMetadata(
            @PathVariable Long documentId,
            @RequestHeader("X-User-Id") Long userId) {
        
        try {
            var documents = documentService.getCaseDocuments(null, userId);
            var document = documents.stream()
                    .filter(doc -> doc.getId().equals(documentId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("Document not found", HttpStatus.NOT_FOUND));

            // Create metadata response
            var metadata = new Object() {
                public final Long id = document.getId();
                public final String fileName = document.getFileName();
                public final String mimeType = document.getMimeType();
                public final Double originalFileSize = document.getOriginalFileSize();
                public final Double storedFileSize = document.getStoredFileSize();
                public final Boolean isEncrypted = document.getIsEncrypted();
                public final Boolean isCompressed = document.getIsCompressed();
                public final String documentType = document.getDocumentType().name();
                public final String uploadedAt = document.getCreatedAt().toString();
            };

            return ResponseEntity.ok(ApiResponse.success(metadata));
            
        } catch (Exception e) {
            log.error("Error getting file metadata {} for user {}: {}", documentId, userId, e.getMessage(), e);
            throw new BusinessException("Failed to get file metadata: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}