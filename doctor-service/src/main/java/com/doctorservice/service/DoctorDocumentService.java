package com.doctorservice.service;

import com.commonlibrary.exception.BusinessException;
import com.doctorservice.dto.*;
import com.doctorservice.entity.Doctor;
import com.doctorservice.entity.DoctorDocument;
import com.doctorservice.repository.DoctorDocumentRepository;
import com.doctorservice.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing doctor verification documents
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorDocumentService {

    private final DoctorDocumentRepository documentRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorDocumentFileStorageService fileStorageService;

    /**
     * Upload a document for doctor verification
     */
    @Transactional
    public DocumentUploadResponseDto uploadDocument(Long userId, MultipartFile file, 
                                                    DoctorDocument.DocumentType documentType,
                                                    String description) {
        try {
            // Find doctor by userId
            Doctor doctor = doctorRepository.findByUserId(userId)
                    .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

            // Validate file
            validateFile(file);

            // Check if document type already exists (for LICENSE and CERTIFICATE, allow only one)
            if (documentType != DoctorDocument.DocumentType.EXPERIENCE) {
                boolean exists = documentRepository.existsByDoctorIdAndDocumentType(doctor.getId(), documentType);
                if (exists) {
                    throw new BusinessException(
                        String.format("%s document already uploaded. Please delete the existing one first.", documentType),
                        HttpStatus.CONFLICT
                    );
                }
            }

            // Store file
            String storedPath = fileStorageService.storeFile(file, doctor.getId(), documentType);
            
            // Calculate checksum
            String checksum = fileStorageService.calculateChecksum(file.getBytes());
            
            // Get stored file size
            long storedSize = fileStorageService.getFileSize(storedPath);

            // Create document entity
            DoctorDocument document = DoctorDocument.builder()
                    .doctor(doctor)
                    .documentType(documentType)
                    .fileName(file.getOriginalFilename())
                    .fileUrl(storedPath)
                    .originalFileSize((double) file.getSize())
                    .storedFileSize((double) storedSize)
                    .mimeType(file.getContentType())
                    .isEncrypted(true)
                    .isCompressed(true)
                    .checksum(checksum)
                    .description(description)
                    .uploadedAt(LocalDateTime.now())
                    .verifiedByAdmin(false)
                    .build();

            // Save document
            document = documentRepository.save(document);

            // Update doctor's profile completion
            updateProfileCompletion(doctor);

            log.info("Document uploaded successfully for doctor {} - Type: {}, File: {}", 
                    doctor.getId(), documentType, file.getOriginalFilename());

            return DocumentUploadResponseDto.builder()
                    .documentId(document.getId())
                    .fileName(document.getFileName())
                    .documentType(documentType.name())
                    .fileSizeKB(document.getOriginalFileSize() / 1024.0)
                    .message("Document uploaded successfully")
                    .fileUrl(fileStorageService.generateFileUrl(document.getId()))
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload document for userId {}: {}", userId, e.getMessage(), e);
            throw new BusinessException("Failed to upload document: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get all documents for a doctor (by userId)
     */
    @Transactional(readOnly = true)
    public DoctorDocumentListDto getMyDocuments(Long userId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        return getDocumentsByDoctorId(doctor.getId());
    }

    /**
     * Get all documents by doctor ID (internal use)
     */
    @Transactional(readOnly = true)
    public DoctorDocumentListDto getDocumentsByDoctorId(Long doctorId) {
        List<DoctorDocument> documents = documentRepository.findByDoctorId(doctorId);
        
        List<DoctorDocumentDto> documentDtos = documents.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        boolean hasAllRequired = hasAllRequiredDocuments(documents);
        boolean allVerified = areAllDocumentsVerified(documents);
        long verifiedCount = documents.stream().filter(DoctorDocument::getVerifiedByAdmin).count();

        return DoctorDocumentListDto.builder()
                .doctorId(doctorId)
                .documents(documentDtos)
                .hasAllRequiredDocuments(hasAllRequired)
                .allDocumentsVerified(allVerified)
                .readyForVerification(hasAllRequired && !allVerified)
                .totalDocuments(documents.size())
                .verifiedDocuments((int) verifiedCount)
                .build();
    }

    /**
     * Get a single document by ID
     */
    @Transactional(readOnly = true)
    public DoctorDocumentDto getDocumentById(Long documentId) {
        DoctorDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("Document not found", HttpStatus.NOT_FOUND));

        return mapToDto(document);
    }

    /**
     * Retrieve document content (for viewing/downloading)
     */
    public byte[] getDocumentContent(Long documentId) {
        try {
            DoctorDocument document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new BusinessException("Document not found", HttpStatus.NOT_FOUND));

            return fileStorageService.retrieveFile(document.getFileUrl());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to retrieve document content for ID {}: {}", documentId, e.getMessage(), e);
            throw new BusinessException("Failed to retrieve document: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete a document (only by the owner doctor)
     */
    @Transactional
    public void deleteDocument(Long userId, Long documentId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        DoctorDocument document = documentRepository.findByIdAndDoctorId(documentId, doctor.getId())
                .orElseThrow(() -> new BusinessException("Document not found or unauthorized", HttpStatus.NOT_FOUND));

        try {
            // Delete physical file
            fileStorageService.deleteFile(document.getFileUrl());

            // Remove document from doctor's collection first (orphanRemoval will handle deletion)
            doctor.getDocuments().remove(document);

            // Update profile completion (this saves the doctor, which triggers orphan removal)
            updateProfileCompletion(doctor);

            log.info("Document deleted successfully - Doctor: {}, Document ID: {}", doctor.getId(), documentId);

        } catch (Exception e) {
            log.error("Failed to delete document {}: {}", documentId, e.getMessage(), e);
            throw new BusinessException("Failed to delete document: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Submit documents for admin review
     */
    @Transactional
    public SubmitDocumentsResponseDto submitDocumentsForReview(Long userId, String additionalNotes) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        // Check if all required documents are uploaded
        boolean hasAllRequired = documentRepository.hasAllRequiredDocuments(doctor.getId());
        
        if (!hasAllRequired) {
            throw new BusinessException(
                "Cannot submit for review. Please upload all required documents (License and Certificate).",
                HttpStatus.BAD_REQUEST
            );
        }

        // Mark as submitted
        doctor.setDocumentsSubmitted(true);
        doctor.setDocumentsSubmittedAt(LocalDateTime.now());
        doctorRepository.save(doctor);

        long documentCount = documentRepository.countByDoctorId(doctor.getId());

        log.info("Doctor {} submitted {} documents for review", doctor.getId(), documentCount);

        // TODO: Send notification to admin about new submission
        // notificationService.notifyAdminAboutDocumentSubmission(doctor);

        return SubmitDocumentsResponseDto.builder()
                .success(true)
                .message("Documents submitted successfully for admin review")
                .documentsSubmitted((int) documentCount)
                .hasAllRequiredDocuments(true)
                .build();
    }

    /**
     * Verify a document (admin only - called via internal endpoint)
     */
    @Transactional
    public void verifyDocument(Long documentId, DocumentVerificationDto verificationDto) {
        DoctorDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("Document not found", HttpStatus.NOT_FOUND));

        document.setVerifiedByAdmin(verificationDto.getVerified());
        document.setVerificationNotes(verificationDto.getVerificationNotes());
        document.setVerifiedBy(verificationDto.getVerifiedBy());
        document.setVerifiedAt(LocalDateTime.now());

        documentRepository.save(document);

        // Update doctor's profile completion
        updateProfileCompletion(document.getDoctor());

        log.info("Document {} verification updated by admin {} - Verified: {}", 
                documentId, verificationDto.getVerifiedBy(), verificationDto.getVerified());
    }

    // ============= PRIVATE HELPER METHODS =============

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File is empty", HttpStatus.BAD_REQUEST);
        }

        if (!fileStorageService.isValidFileType(file.getContentType())) {
            throw new BusinessException(
                "Invalid file type. Only PDF, JPG, JPEG, and PNG are allowed.",
                HttpStatus.BAD_REQUEST
            );
        }

        if (!fileStorageService.isValidFileSize(file.getSize())) {
            throw new BusinessException(
                "File size exceeds maximum limit of 5MB",
                HttpStatus.BAD_REQUEST
            );
        }
    }

    /**
     * Check if doctor has all required documents
     */
    private boolean hasAllRequiredDocuments(List<DoctorDocument> documents) {
        boolean hasLicense = documents.stream()
                .anyMatch(d -> d.getDocumentType() == DoctorDocument.DocumentType.LICENSE);
        boolean hasCertificate = documents.stream()
                .anyMatch(d -> d.getDocumentType() == DoctorDocument.DocumentType.CERTIFICATE);
        
        return hasLicense && hasCertificate;
    }

    /**
     * Check if all required documents are verified
     */
    private boolean areAllDocumentsVerified(List<DoctorDocument> documents) {
        return documents.stream()
                .filter(d -> d.getDocumentType() == DoctorDocument.DocumentType.LICENSE || 
                            d.getDocumentType() == DoctorDocument.DocumentType.CERTIFICATE)
                .allMatch(DoctorDocument::getVerifiedByAdmin);
    }

    /**
     * Update doctor's profile completion percentage
     */
    private void updateProfileCompletion(Doctor doctor) {
        int completion = doctor.calculateProfileCompletion();
        doctor.setProfileCompletionPercentage(completion);
        doctorRepository.save(doctor);
        log.debug("Updated profile completion for doctor {}: {}%", doctor.getId(), completion);
    }

    /**
     * Map DoctorDocument entity to DTO
     */
    private DoctorDocumentDto mapToDto(DoctorDocument document) {
        return DoctorDocumentDto.builder()
                .id(document.getId())
                .doctorId(document.getDoctor().getId())
                .documentType(document.getDocumentType().name())
                .fileName(document.getFileName())
                .fileUrl(document.getFileUrl())
                .fileSizeKB(document.getOriginalFileSize() != null ? document.getOriginalFileSize() / 1024.0 : 0)
                .mimeType(document.getMimeType())
                .isEncrypted(document.getIsEncrypted())
                .isCompressed(document.getIsCompressed())
                .description(document.getDescription())
                .uploadedAt(document.getUploadedAt())
                .verifiedByAdmin(document.getVerifiedByAdmin())
                .verifiedAt(document.getVerifiedAt())
                .verifiedBy(document.getVerifiedBy())
                .verificationNotes(document.getVerificationNotes())
                .downloadUrl(fileStorageService.generateDownloadUrl(document.getId()))
                .viewUrl(fileStorageService.generateFileUrl(document.getId()))
                .build();
    }
}