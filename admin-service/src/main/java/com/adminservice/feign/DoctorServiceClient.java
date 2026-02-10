package com.adminservice.feign;

import com.adminservice.dto.DoctorDetailsDto;
import com.commonlibrary.dto.*;
import com.commonlibrary.entity.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@FeignClient(name = "doctor-service")
public interface DoctorServiceClient {

    @PutMapping("/api/doctors/{doctorId}/verification")
    ResponseEntity<ApiResponse<Void>> updateDoctorVerification(@PathVariable Long doctorId,
                                                               @RequestParam String status,
                                                               @RequestParam String reason);

    @GetMapping("/api/doctors/{doctorId}/verification-details")
    ResponseEntity<ApiResponse<DoctorVerificationDetailsDto>> getDoctorVerificationDetails(
            @PathVariable("doctorId") Long doctorId);

    @GetMapping("/api/doctors/status/pending-verifications")
    ResponseEntity<ApiResponse<List<PendingVerificationDto>>> getPendingVerifications();

    @GetMapping("/api/doctors/pending-verifications/count")
    ResponseEntity<ApiResponse<Long>> getPendingVerificationsCount();

    @GetMapping("/api/profile/{doctorId}")
    ResponseEntity<ApiResponse<DoctorProfileDto>> getDoctorDetails(@PathVariable Long doctorId);

    @GetMapping("/api/doctors/{doctorId}/performance")
    ResponseEntity<ApiResponse<Map<String, Object>>> getDoctorPerformance(@PathVariable Long doctorId,
                                             @RequestParam LocalDate startDate,
                                             @RequestParam LocalDate endDate);
    // ========== NEW METHODS ==========

    /**
     * Verify Doctor (Approve/Reject)
     * POST /api/doctors/{doctorId}/verify
     */
    @PostMapping("/api/doctors/{doctorId}/verify")
    ResponseEntity<ApiResponse<DoctorVerificationResponseDto>> verifyDoctor(
            @PathVariable("doctorId") Long doctorId,
            @RequestBody VerifyDoctorRequestDto request);

    /**
     * Reject Doctor Verification
     * POST /api/doctors/{doctorId}/reject
     */
    @PostMapping("/api/doctors/{doctorId}/reject")
    ResponseEntity<ApiResponse<DoctorVerificationResponseDto>> rejectDoctor(
            @PathVariable("doctorId") Long doctorId,
            @RequestBody RejectDoctorRequestDto request);

    /**
     * Update Doctor Status
     * PUT /api/doctors/{doctorId}/status
     */
    @PutMapping("/api/doctors/{doctorId}/status")
    ResponseEntity<ApiResponse<Void>> updateDoctorStatus(
            @PathVariable("doctorId") Long doctorId,
            @RequestBody UpdateDoctorStatusRequestDto request);

    /**
     * Get All Doctors with Filters
     * GET /api/doctors
     */
    @GetMapping("/api/doctors")
    ResponseEntity<ApiResponse<Page<DoctorSummaryDto>>> getAllDoctors(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) VerificationStatus verificationStatus,
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) Boolean isAvailable,
            @RequestParam(required = false) Boolean emergencyMode,
            @RequestParam(required = false) Integer minYearsExperience,
            @RequestParam(required = false) Integer maxYearsExperience,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection);

    /**
     * Get complete doctor details by doctor ID
     * Used for validation during case assignment
     */
    @GetMapping("/api/doctors-internal/{doctorId}")
    ResponseEntity<ApiResponse<?>> getDoctorById(@PathVariable Long doctorId);

    /**
     * Get basic doctor information (name, specialization, verification status)
     * Used for displaying doctor info in admin case management
     */
    @GetMapping("/api/doctors-internal/{doctorId}/basic-info")
    ResponseEntity<ApiResponse<Map<String, Object>>> getDoctorBasicInfo(@PathVariable Long doctorId);

    /**
     * Get doctor profile details
     */
    @GetMapping("/api/doctors-internal/profile/{doctorId}")
    ResponseEntity<ApiResponse<?>> getDoctorProfile(@PathVariable Long doctorId);

    /**
     * Get documents for a specific doctor
     * GET /api/doctors-internal/{doctorId}/documents
     */
    @GetMapping("/api/doctors-internal/{doctorId}/documents")
    ResponseEntity<ApiResponse<DoctorDocumentListDto>> getDoctorDocuments(
            @PathVariable("doctorId") Long doctorId);

    /**
     * Get document content for viewing
     * GET /api/doctors-internal/documents/{documentId}/content
     */
    @GetMapping("/api/doctors-internal/documents/{documentId}/content")
    ResponseEntity<byte[]> getDocumentContent(
            @PathVariable("documentId") Long documentId);

    /**
     * Download document
     * GET /api/doctors-internal/documents/{documentId}/download
     */
    @GetMapping("/api/doctors-internal/documents/{documentId}/download")
    ResponseEntity<byte[]> downloadDocument(
            @PathVariable("documentId") Long documentId);

    /**
     * Verify a document
     * PUT /api/doctors-internal/documents/{documentId}/verify
     */
    @PutMapping("/api/doctors-internal/documents/{documentId}/verify")
    ResponseEntity<ApiResponse<Void>> verifyDocument(
            @PathVariable("documentId") Long documentId,
            @RequestBody DocumentVerificationDto request);

// ============= DTOs to add to admin-service (create new files or add to existing DTO package) =============

    /**
     * DTO for document list response
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

    /**
     * DTO for single document
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public class DoctorDocumentDto {
        private Long id;
        private Long doctorId;
        private String documentType;
        private String fileName;
        private String fileUrl;
        private Double fileSizeKB;
        private String mimeType;
        private Boolean isEncrypted;
        private Boolean isCompressed;
        private String description;
        private LocalDateTime uploadedAt;
        private Boolean verifiedByAdmin;
        private LocalDateTime verifiedAt;
        private Long verifiedBy;
        private String verificationNotes;
        private String downloadUrl;
        private String viewUrl;
    }

    /**
     * DTO for document verification request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public class DocumentVerificationDto {
        private Boolean verified;
        private String verificationNotes;
        private Long verifiedBy; // Admin user ID
    }

}
