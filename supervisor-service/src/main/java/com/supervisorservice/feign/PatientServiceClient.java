package com.supervisorservice.feign;

import com.commonlibrary.dto.*;
import com.supervisorservice.dto.CreateCaseRequest;
import com.supervisorservice.dto.PatientDto;
import com.supervisorservice.dto.UpdateCaseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign client for patient-service
 */
@FeignClient(
    name = "patient-service")
public interface PatientServiceClient {

    /**
     * Get patient basic info (internal endpoint)
     */
    @GetMapping("/api/patients-internal/{patientId}/basic-info")
    ApiResponse<Map<String, Object>> getPatientBasicInfo(@PathVariable Long patientId);

    /**
     * Get all cases for admin with filters (internal endpoint)
     */
    @GetMapping("/api/patients-internal/cases/admin/all")
    ApiResponse<List<CaseDto>> getAllCasesForAdmin(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String urgencyLevel,
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String searchTerm);

    /**
     * Get specific case details (internal endpoint)
     */
    @GetMapping("/api/patients-internal/cases/{caseId}")
    ApiResponse<CaseDto> getCaseDetails(@PathVariable Long caseId);

    /**
     * Get case statistics by status (internal endpoint)
     */
    @GetMapping("/api/patients-internal/cases/statistics/by-status")
    ApiResponse<Map<String, Long>> getCaseStatisticsByStatus();

    /**
     * Get case statistics by urgency (internal endpoint)
     */
    @GetMapping("/api/patients-internal/cases/statistics/by-urgency")
    ApiResponse<Map<String, Long>> getCaseStatisticsByUrgency();

    /**
     * Get case statistics by specialization (internal endpoint)
     */
    @GetMapping("/api/patients-internal/cases/statistics/by-specialization")
    ApiResponse<Map<String, Long>> getCaseStatisticsBySpecialization();

    /**
     * Create case (JSON-based, files can be uploaded separately)
     * Note: This is a custom endpoint that needs to be added to patient-service
     * Or we use the existing multipart endpoint with JSON conversion
     */
    @PostMapping(value = "/api/patients/cases/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<CaseDto>> createCaseJson(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody CreateCaseDto request);

    /**
     * Update case
     */
    @PutMapping("/api/patients/cases/{caseId}")
    ApiResponse<Void> updateCase(
            @PathVariable Long caseId,
            @RequestBody UpdateCaseDto updatedCase);

    /**
     * Update case status
     */
    @PutMapping("/api/patients/cases/{caseId}/status")
    ApiResponse<Void> updateCaseStatus(
            @PathVariable Long caseId,
            @RequestParam String status,
            @RequestParam Long doctorId);

    /**
     * Delete case
     */
    @PutMapping("/api/patients/cases/{caseId}/delete")
    ApiResponse<Void> deleteCase(
            @PathVariable Long caseId,
            @RequestHeader("X-User-Id") Long userId);


        /**
         * Get patient by ID
         */
        @GetMapping("/api/patients/{patientId}")
        PatientDto getPatient(@PathVariable Long patientId);

        /**
         * Get patient by user ID
         */
        @GetMapping("/api/patients/user/{userId}")
        PatientProfileDto getPatientByUserId(@PathVariable Long userId);

        /**
         * Get patient by email (NEW - for assigning existing patients)
         */
        @GetMapping("/api/patients/by-email")
        ResponseEntity<ApiResponse<PatientProfileDto>> getPatientByEmail(@RequestParam String email);

        /**
         * Create patient profile by supervisor (NEW - for creating new patients)
         * This will create both user account and patient profile
         */
        @PostMapping("/api/patients/create-by-supervisor")
        ResponseEntity<ApiResponse<PatientProfileDto>> createPatientBySupervisor(
                @RequestBody CreatePatientProfileRequest request,
                @RequestHeader("X-Supervisor-Id") Long supervisorId);


        /**
         * Get all cases for a patient
         */
        @GetMapping("/api/patients/{patientId}/cases")
        List<CaseDto> getPatientCases(@PathVariable Long patientId);

        /**
         * Get active cases count for patient
         */
        @GetMapping("/api/patients/{patientId}/cases/active/count")
        Long getActiveCasesCount(@PathVariable Long patientId);

    @GetMapping("/api/patients/cases/{caseId}/custom-info")
    ResponseEntity<ApiResponse<CustomPatientDto>> getCustomPatientInfo( @PathVariable Long caseId,
                                                                        @RequestParam Long doctorId );

}