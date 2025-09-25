package com.patientservice.service;

import com.commonlibrary.entity.CaseStatus;
import com.commonlibrary.entity.DocumentType;
import com.commonlibrary.exception.BusinessException;
import com.patientservice.dto.CaseAttachmentsDto;
import com.patientservice.entity.Case;
import com.patientservice.entity.Document;
import com.patientservice.repository.CaseRepository;
import com.patientservice.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final FileValidationService fileValidationService;
    private final CaseRepository caseRepository;

    /**
     * Process and save uploaded files for a case
     */
    public List<Document> processAndSaveFiles(List<MultipartFile> files, Case medicalCase, Long uploadedBy) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        // Validate all files first
        fileValidationService.validateFiles(files);

        List<Document> savedDocuments = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                Document document = processAndSaveFile(file, medicalCase, uploadedBy);
                savedDocuments.add(document);
            } catch (Exception e) {
                log.error("Error processing file: {}", file.getOriginalFilename(), e);
                // Clean up any successfully saved files if one fails
                cleanupDocuments(savedDocuments);
                throw new BusinessException(
                    String.format("Failed to process file '%s': %s", file.getOriginalFilename(), e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
                );
            }
        }

        log.info("Successfully processed {} files for case {}", savedDocuments.size(), medicalCase.getId());
        return savedDocuments;
    }

    /**
     * Process and save a single file
     */
    private Document processAndSaveFile(MultipartFile file, Case medicalCase, Long uploadedBy) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String mimeType = file.getContentType();
        
        // Store the file (compressed and encrypted)
        String storedPath = fileStorageService.storeFile(file, medicalCase.getId());
        
        // Determine document type based on MIME type
        DocumentType documentType = determineDocumentType(mimeType);
        
        // Calculate checksum for integrity verification
        String checksum = calculateChecksum(file.getBytes());
        
        // Create document entity
        Document document = Document.builder()
                .medicalCase(medicalCase)
                .uploadedBy(uploadedBy)
                .documentType(documentType)
                .fileName(originalFilename)
                .fileUrl(storedPath)
                .originalFileSize((double) file.getSize())
                .storedFileSize((double) fileStorageService.getFileSize(storedPath))
                .mimeType(mimeType)
                .isVerified(false)
                .isEncrypted(true)
                .isCompressed(true)
                .checksum(checksum)
                .accessUrl(fileStorageService.generateFileUrl(null)) // Will be updated after save
                .build();

        // Save to database
        document = documentRepository.save(document);
        
        // Update access URL with actual document ID
        document.setAccessUrl(fileStorageService.generateFileUrl(document.getId()));
        document = documentRepository.save(document);

        log.info("Document saved: {} -> ID: {}", originalFilename, document.getId());
        return document;
    }

    /**
     * Retrieve file content for viewing
     */
    public byte[] getFileContent(Long documentId, Long userId) throws Exception {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("Document not found", HttpStatus.NOT_FOUND));

        // Verify user has access to this document
        if (!hasAccessToDocument(document, userId)) {
            throw new BusinessException("Unauthorized access to document", HttpStatus.FORBIDDEN);
        }

        // Retrieve and decrypt file
        byte[] fileContent = fileStorageService.retrieveFile(document.getFileUrl());
        
        log.info("File content retrieved for document ID: {} by user: {}", documentId, userId);
        return fileContent;
    }

    /**
     * Get documents for a case (for case details view)
     */
    public List<Document> getCaseDocuments(Long caseId, Long userId) {
        List<Document> documents = documentRepository.findByCaseId(caseId);
        
        // Additional security check can be added here
        log.info("Retrieved {} documents for case: {} by user: {}", documents.size(), caseId, userId);
        return documents;
    }

    public Document getDocumentById(Long id) {
        Document document = documentRepository.findById(id).orElse(null);

        // Additional security check can be added here
        if(document == null) {
            log.info("Failed retrieved  document with di {} for case: {}", document.getId(),
                    document.getMedicalCase().getId());
            System.out.println("Failed to retrieved  document with di: " +  document.getId() +
                    ", for case: "+document.getMedicalCase().getId());
        }

        return document;
    }

    /**
     * Delete a document
     */
    public void deleteDocument(Long documentId, Long userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("Document not found", HttpStatus.NOT_FOUND));

        if (!hasAccessToDocument(document, userId)) {
            throw new BusinessException("Unauthorized access to document", HttpStatus.FORBIDDEN);
        }

        // Delete physical file
        boolean fileDeleted = fileStorageService.deleteFile(document.getFileUrl());
        if (!fileDeleted) {
            log.warn("Physical file could not be deleted: {}", document.getFileUrl());
        }

        // Delete database record
        documentRepository.delete(document);
        log.info("Document deleted: ID: {} by user: {}", documentId, userId);
    }

    /**
     * Determine document type based on MIME type
     */
    private DocumentType determineDocumentType(String mimeType) {
        if (mimeType == null) {
            return DocumentType.OTHER;
        }

        if (mimeType.equals("application/pdf")) {
            return DocumentType.MEDICAL_REPORT; // Default PDF to medical report
        } else if (mimeType.startsWith("image/")) {
            return DocumentType.IMAGING; // Images are typically medical imaging
        }

        return DocumentType.OTHER;
    }

    /**
     * Calculate MD5 checksum for file integrity
     */
    private String calculateChecksum(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] checksum = md.digest(data);
        return Base64.getEncoder().encodeToString(checksum);
    }

    /**
     * Check if user has access to document
     */
    private boolean hasAccessToDocument(Document document, Long userId) {
        // Patient can access their own case documents
        if (document.getMedicalCase().getPatient().getUserId().equals(userId)) {
            return true;
        }

        /*TODO */
        // Doctor can access if assigned to the case
        // This would require checking case assignments
        // For now, allow if user uploaded the document
        return document.getUploadedBy().equals(userId);
    }

    /**
     * Clean up documents in case of processing failure
     */
    private void cleanupDocuments(List<Document> documents) {
        for (Document doc : documents) {
            try {
                if (doc.getId() != null) {
                    fileStorageService.deleteFile(doc.getFileUrl());
                    documentRepository.delete(doc);
                }
            } catch (Exception e) {
                log.error("Error cleaning up document: {}", doc.getId(), e);
            }
        }
    }

    // Add these methods to DocumentService.java

    /**
     * Add additional files to an existing case
     */
    public List<Document> addFilesToCase(List<MultipartFile> files, Long caseId, Long uploadedBy) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException("No files provided for upload", HttpStatus.BAD_REQUEST);
        }

        // Get existing documents for the case
        List<Document> existingDocuments = documentRepository.findByCaseId(caseId);

        // Validate files can be added considering existing count
        fileValidationService.validateFilesForCaseUpdate(files, existingDocuments.size());

        // Get the case entity
        Case medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new BusinessException("Case not found", HttpStatus.NOT_FOUND));

        List<Document> newDocuments = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                Document document = processAndSaveFile(file, medicalCase, uploadedBy);
                newDocuments.add(document);
                log.info("Additional file uploaded for case {}: {}", caseId, file.getOriginalFilename());
            } catch (Exception e) {
                log.error("Error processing additional file for case {}: {}", caseId, file.getOriginalFilename(), e);
                // Clean up any successfully saved files if one fails
                cleanupDocuments(newDocuments);
                throw new BusinessException(
                        String.format("Failed to process file '%s': %s", file.getOriginalFilename(), e.getMessage()),
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }
        }

        log.info("Successfully added {} files to case {}", newDocuments.size(), caseId);
        return newDocuments;
    }

    /**
     * Get case attachment summary
     */
    public CaseAttachmentsDto getCaseAttachmentsSummary(Long caseId, Long userId) {
        List<Document> documents = getCaseDocuments(caseId, userId);

        // Calculate statistics
        long totalSize = documents.stream()
                .mapToLong(doc -> doc.getOriginalFileSize() != null ? doc.getOriginalFileSize().longValue() : 0L)
                .sum();

        int remainingSlots = fileValidationService.getRemainingFileSlots(documents.size());

        LocalDateTime lastUploadTime = documents.stream()
                .map(Document::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        // Map documents to summary DTOs
        List<CaseAttachmentsDto.DocumentSummaryDto> documentSummaries = documents.stream()
                .map(this::mapToDocumentSummary)
                .collect(Collectors.toList());

        return CaseAttachmentsDto.builder()
                .caseId(caseId)
                .totalDocuments(documents.size())
                .totalSizeBytes(totalSize)
                .remainingSlots(remainingSlots)
                .lastUploadTime(lastUploadTime)
                .documents(documentSummaries)
                .build();
    }

    /**
     * Create attachment summary after adding new files
     */
    public CaseAttachmentsDto createAttachmentsSummaryWithNewFiles(Long caseId, Long userId, List<Document> newDocuments) {
        CaseAttachmentsDto summary = getCaseAttachmentsSummary(caseId, userId);

        // Set case title if available
        if (!newDocuments.isEmpty()) {
            summary.setCaseTitle(newDocuments.get(0).getMedicalCase().getCaseTitle());
        }

        // Mark new documents and set count
        summary.setNewDocumentsUploaded(newDocuments.size());

        List<CaseAttachmentsDto.DocumentSummaryDto> newDocumentSummaries = newDocuments.stream()
                .map(doc -> {
                    CaseAttachmentsDto.DocumentSummaryDto summary_dto = mapToDocumentSummary(doc);
                    summary_dto.setIsNewUpload(true);
                    return summary_dto;
                })
                .collect(Collectors.toList());

        summary.setNewDocuments(newDocumentSummaries);

        // Update remaining slots after new uploads
        summary.setRemainingSlots(fileValidationService.getRemainingFileSlots(summary.getTotalDocuments()));

        return summary;
    }

    /**
     * Map Document to DocumentSummaryDto
     */
    private CaseAttachmentsDto.DocumentSummaryDto mapToDocumentSummary(Document document) {
        return CaseAttachmentsDto.DocumentSummaryDto.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .mimeType(document.getMimeType())
                .fileSizeKB(document.getOriginalFileSize() != null ? document.getOriginalFileSize() / 1024 : 0)
                .documentType(document.getDocumentType().name())
                .accessUrl(String.format("/api/files/%d", document.getId()))
                .downloadUrl(String.format("/api/patients/documents/%d/download", document.getId()))
                .isEncrypted(document.getIsEncrypted())
                .isCompressed(document.getIsCompressed())
                .uploadedAt(document.getCreatedAt())
                .description(document.getDescription())
                .isNewUpload(false) // Default to false, will be set to true for new uploads
                .build();
    }

    /**
     * Validate user has permission to add files to case
     */
    public void validateCaseFileAccess(Long caseId, Long userId) {
        Case medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new BusinessException("Case not found", HttpStatus.NOT_FOUND));

        // Check if user is the case owner
        if (!medicalCase.getPatient().getUserId().equals(userId)) {
            throw new BusinessException("Unauthorized access to case", HttpStatus.FORBIDDEN);
        }

        // Check if case is in a state that allows file uploads
        if (medicalCase.getStatus() == CaseStatus.CLOSED ||
                medicalCase.getStatus() == CaseStatus.REJECTED ||
                medicalCase.getStatus() == CaseStatus.CONSULTATION_COMPLETE
        ) {
            throw new BusinessException("Cannot upload files to closed or cancelled cases", HttpStatus.BAD_REQUEST);
        }
    }
}