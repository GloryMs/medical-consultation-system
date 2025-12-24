package com.adminservice.feign;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.CaseAnalyticsDto;
import com.commonlibrary.dto.CaseDto;
import com.commonlibrary.dto.CaseMetricsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "patient-service")
public interface PatientServiceClient {
    /**
     * Get all cases for admin with filters
     */
    @GetMapping("/api/patients-internal/cases/admin/all")
    ResponseEntity<ApiResponse<List<CaseDto>>> getAllCasesForAdmin(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String urgencyLevel,
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String searchTerm
    );

    @GetMapping("/api/patients-internal/cases/analytics")
    ResponseEntity<ApiResponse<CaseAnalyticsDto>> getCaseAnalytics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    );

    /**
     * Get case by ID
     */
    @GetMapping("/api/patients-internal/cases/{caseId}")
    ResponseEntity<ApiResponse<CaseDto>> getCaseById(@PathVariable Long caseId);

    /**
     * Get case assignment history
     */
    @GetMapping("/api/patients-internal/cases/{caseId}/assignments/history")
    ResponseEntity<ApiResponse<List<?>>> getCaseAssignmentHistory(@PathVariable Long caseId);

    /**
     * Create case assignment
     */
    @PostMapping("/api/patients-internal/case-assignments")
    ResponseEntity<ApiResponse<?>> createCaseAssignment(@RequestBody Map<String, Object> assignmentRequest);

    /**
     * Update assignment status
     */
    @PutMapping("/api/patients-internal/case-assignments/status")
    ResponseEntity<ApiResponse<?>> updateAssignmentStatus(@RequestBody Map<String, Object> updateRequest);

    /**
     * Update case status
     */
    @PutMapping("/api/patients-internal/cases/{caseId}/status")
    ResponseEntity<ApiResponse<?>> updateCaseStatus(
            @PathVariable Long caseId,
            @RequestBody Map<String, Object> statusUpdate
    );

    /**
     * Get case metrics
     */
    @GetMapping("/api/patients-internal/cases/metrics")
    ResponseEntity<ApiResponse<CaseMetricsDto>> getCaseMetrics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    );

    /**
     * Get unassigned cases
     */
    @GetMapping("/api/patients-internal/cases/unassigned")
    ResponseEntity<ApiResponse<List<CaseDto>>> getUnassignedCases(
            @RequestParam(required = false) String urgencyLevel,
            @RequestParam(required = false) String specialization
    );

    /**
     * Get overdue cases
     */
    @GetMapping("/api/patients-internal/cases/overdue")
    ResponseEntity<ApiResponse<List<CaseDto>>> getOverdueCases();

    /**
     * Get patient basic info
     */
    @GetMapping("/api/patients-internal/{patientId}/basic-info")
    ResponseEntity<ApiResponse<?>> getPatientBasicInfo(@PathVariable Long patientId);


}
