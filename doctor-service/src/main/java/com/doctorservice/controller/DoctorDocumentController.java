package com.doctorservice.controller;

import com.commonlibrary.dto.ApiResponse;
import com.doctorservice.dto.*;
import com.doctorservice.entity.DoctorDocument;
import com.doctorservice.service.DoctorDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller for doctor document management
 * Handles document upload, retrieval, and deletion for doctors
 */
@RestController
@RequestMapping("/api/doctors/profile/documents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Doctor Documents", description = "Doctor verification document management")
public class DoctorDocumentController {

    private final DoctorDocumentService documentService;

    /**
     * Upload a verification document
     * POST /api/doctors/profile/documents/upload
     */
    @PostMapping("/upload")
    @Operation(summary = "Upload verification document", description = "Upload medical license, certificate, or experience document")
    public ResponseEntity<ApiResponse<DocumentUploadResponseDto>> uploadDocument(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "description", required = false) String description) {

        log.info("Document upload request - User: {}, Type: {}, File: {}", userId, documentType, file.getOriginalFilename());

        DoctorDocument.DocumentType type;
        try {
            type = DoctorDocument.DocumentType.valueOf(documentType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid document type. Must be LICENSE, CERTIFICATE, or EXPERIENCE",
                            HttpStatus.BAD_REQUEST));
        }

        DocumentUploadResponseDto response = documentService.uploadDocument(userId, file, type, description);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Document uploaded successfully"));
    }

    /**
     * Get all documents for the authenticated doctor
     * GET /api/doctors/profile/documents
     */
    @GetMapping
    @Operation(summary = "Get my documents", description = "Retrieve all uploaded documents for the authenticated doctor")
    public ResponseEntity<ApiResponse<DoctorDocumentListDto>> getMyDocuments(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Get documents request - User: {}", userId);
        
        DoctorDocumentListDto documents = documentService.getMyDocuments(userId);
        
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    /**
     * Get a specific document by ID
     * GET /api/doctors/profile/documents/{documentId}
     */
    @GetMapping("/{documentId}")
    @Operation(summary = "Get document by ID", description = "Retrieve specific document details")
    public ResponseEntity<ApiResponse<DoctorDocumentDto>> getDocument(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long documentId) {

        log.info("Get document request - User: {}, Document: {}", userId, documentId);
        
        DoctorDocumentDto document = documentService.getDocumentById(documentId);
        
        return ResponseEntity.ok(ApiResponse.success(document));
    }

    /**
     * Download document content
     * GET /api/doctors/profile/documents/{documentId}/download
     */
    @GetMapping("/{documentId}/download")
    @Operation(summary = "Download document", description = "Download document file")
    public ResponseEntity<byte[]> downloadDocument(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long documentId) {

        log.info("Download document request - User: {}, Document: {}", userId, documentId);
        
        DoctorDocumentDto document = documentService.getDocumentById(documentId);
        byte[] content = documentService.getDocumentContent(documentId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(document.getMimeType()));
        headers.setContentDispositionFormData("attachment", document.getFileName());
        headers.setContentLength(content.length);

        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }

    /**
     * View document content (inline)
     * GET /api/doctors/profile/documents/{documentId}/view
     */
    @GetMapping("/{documentId}/view")
    @Operation(summary = "View document", description = "View document content inline")
    public ResponseEntity<byte[]> viewDocument(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long documentId) {

        log.info("View document request - User: {}, Document: {}", userId, documentId);
        
        DoctorDocumentDto document = documentService.getDocumentById(documentId);
        byte[] content = documentService.getDocumentContent(documentId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(document.getMimeType()));
        headers.setContentDispositionFormData("inline", document.getFileName());
        headers.setContentLength(content.length);

        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }

    /**
     * Delete a document
     * DELETE /api/doctors/profile/documents/{documentId}
     */
    @DeleteMapping("/{documentId}")
    @Operation(summary = "Delete document", description = "Delete uploaded document")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long documentId) {

        log.info("Delete document request - User: {}, Document: {}", userId, documentId);
        
        documentService.deleteDocument(userId, documentId);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Document deleted successfully"));
    }

    /**
     * Submit documents for admin review
     * POST /api/doctors/profile/documents/submit
     */
    @PostMapping("/submit")
    @Operation(summary = "Submit documents for review", description = "Submit all uploaded documents for admin verification")
    public ResponseEntity<ApiResponse<SubmitDocumentsResponseDto>> submitForReview(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody(required = false) SubmitDocumentsRequestDto request) {

        log.info("Submit documents for review - User: {}", userId);
        
        String notes = request != null ? request.getAdditionalNotes() : null;
        SubmitDocumentsResponseDto response = documentService.submitDocumentsForReview(userId, notes);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Documents submitted for review"));
    }
}