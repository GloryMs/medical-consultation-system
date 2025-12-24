package com.patientservice.controller;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.CaseAnalyticsDto;
import com.commonlibrary.dto.CaseDto;
import com.commonlibrary.dto.CaseMetricsDto;
import com.commonlibrary.entity.CaseStatus;
import com.patientservice.dto.CaseDetailsDto;
import com.patientservice.service.CaseAnalyticsService;
import com.patientservice.service.PatientAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for admin operations on patient cases
 * These endpoints are called by admin-service via Feign
 */
@RestController
@RequestMapping("/api/patients-internal")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Patient Admin Operations", description = "Admin endpoints for patient case management")
public class PatientAdminController {

    private final PatientAdminService patientAdminService;
    private final CaseAnalyticsService caseAnalyticsService;


    @GetMapping("/cases/analytics")
    @Operation(summary = "Get comprehensive case analytics")
    public ResponseEntity<ApiResponse<CaseAnalyticsDto>> getCaseAnalytics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        CaseAnalyticsDto analytics = caseAnalyticsService.getCaseAnalytics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(analytics, "Analytics retrieved"));
    }


    /**
     * Get all cases for admin with comprehensive filtering
     * Called by admin-service to retrieve cases for admin panel
     */
    @GetMapping("/cases/admin/all")
    @Operation(summary = "Get all cases for admin", 
               description = "Retrieve all cases with filtering for admin panel")
    public ResponseEntity<ApiResponse<List<CaseDto>>> getAllCasesForAdmin(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String urgencyLevel,
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String searchTerm) {
        
        log.info("Admin fetching all cases with filters - status: {}, urgency: {}, spec: {}", 
                 status, urgencyLevel, specialization);
        
        List<CaseDto> cases = patientAdminService.getAllCasesForAdmin(
                status, urgencyLevel, specialization, patientId, doctorId, 
                startDate, endDate, searchTerm
        );
        
        return ResponseEntity.ok(ApiResponse.success(cases, 
                String.format("Retrieved %d cases", cases.size())));
    }

    /**
     * Get case assignment history
     */
    @GetMapping("/cases/{caseId}/assignments/history")
    @Operation(summary = "Get case assignment history", 
               description = "Retrieve all assignment attempts for a case")
    public ResponseEntity<ApiResponse<List<?>>> getCaseAssignmentHistory(@PathVariable Long caseId) {
        log.info("Fetching assignment history for case {}", caseId);
        
        List<?> assignmentHistory = patientAdminService.getCaseAssignmentHistory(caseId);
        
        return ResponseEntity.ok(ApiResponse.success(assignmentHistory, 
                "Assignment history retrieved successfully"));
    }

    /**
     * Create case assignment (admin initiated)
     */
    @PostMapping("/case-assignments")
    @Operation(summary = "Create case assignment", 
               description = "Create a new case assignment (admin operation)")
    public ResponseEntity<ApiResponse<?>> createCaseAssignment(
            @RequestBody Map<String, Object> assignmentRequest) {
        
        log.info("Creating case assignment: {}", assignmentRequest);
        
        patientAdminService.createCaseAssignment(assignmentRequest);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Assignment created successfully"));
    }

    /**
     * Update assignment status
     */
    @PutMapping("/case-assignments/status")
    @Operation(summary = "Update assignment status", 
               description = "Update the status of a case assignment")
    public ResponseEntity<ApiResponse<?>> updateAssignmentStatus(
            @RequestBody Map<String, Object> updateRequest) {
        
        log.info("Updating assignment status: {}", updateRequest);
        
        patientAdminService.updateAssignmentStatus(updateRequest);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Assignment status updated"));
    }

    /**
     * Update case status (admin override)
     */
    @PutMapping("/cases/{caseId}/status")
    @Operation(summary = "Update case status", 
               description = "Admin override to update case status")
    public ResponseEntity<ApiResponse<?>> updateCaseStatusAdmin(
            @PathVariable Long caseId,
            @RequestBody Map<String, Object> statusUpdate) {
        
        log.info("Admin updating case {} status: {}", caseId, statusUpdate);
        
        patientAdminService.updateCaseStatusAdmin(caseId, statusUpdate);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Case status updated"));
    }

    @GetMapping("/cases/{caseId}")
    public ResponseEntity<ApiResponse<CaseDto>> getCaseDetails(
            @PathVariable Long caseId) {
        CaseDto caseDetails = patientAdminService.getCaseDetails(caseId);
        return ResponseEntity.ok(ApiResponse.success(caseDetails));
    }

    /**
     * Get case metrics for admin dashboard
     */
    @GetMapping("/cases/metrics")
    @Operation(summary = "Get case metrics", 
               description = "Retrieve comprehensive case metrics and statistics")
    public ResponseEntity<ApiResponse<CaseMetricsDto>> getCaseMetrics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        log.info("Fetching case metrics from {} to {}", startDate, endDate);
        
        CaseMetricsDto metrics = patientAdminService.getCaseMetrics(startDate, endDate);
        
        return ResponseEntity.ok(ApiResponse.success(metrics, "Metrics retrieved successfully"));
    }

    /**
     * Get unassigned cases
     */
    @GetMapping("/cases/unassigned")
    @Operation(summary = "Get unassigned cases", 
               description = "Retrieve cases pending doctor assignment")
    public ResponseEntity<ApiResponse<List<CaseDto>>> getUnassignedCases(
            @RequestParam(required = false) String urgencyLevel,
            @RequestParam(required = false) String specialization) {
        
        log.info("Fetching unassigned cases - urgency: {}, spec: {}", urgencyLevel, specialization);
        
        List<CaseDto> cases = patientAdminService.getUnassignedCases(urgencyLevel, specialization);
        
        return ResponseEntity.ok(ApiResponse.success(cases, 
                String.format("Retrieved %d unassigned cases", cases.size())));
    }

    /**
     * Get overdue cases
     */
    @GetMapping("/cases/overdue")
    @Operation(summary = "Get overdue cases", 
               description = "Retrieve cases past their expected completion date")
    public ResponseEntity<ApiResponse<List<CaseDto>>> getOverdueCases() {
        log.info("Fetching overdue cases");
        
        List<CaseDto> cases = patientAdminService.getOverdueCases();
        
        return ResponseEntity.ok(ApiResponse.success(cases, 
                String.format("Retrieved %d overdue cases", cases.size())));
    }

    /**
     * Get patient basic info
     */
    @GetMapping("/{patientId}/basic-info")
    @Operation(summary = "Get patient basic info", 
               description = "Retrieve basic patient information for display")
    public ResponseEntity<ApiResponse<?>> getPatientBasicInfo(@PathVariable Long patientId) {
        log.info("Fetching basic info for patient {}", patientId);
        
        Map<String, Object> patientInfo = patientAdminService.getPatientBasicInfo(patientId);
        
        return ResponseEntity.ok(ApiResponse.success(patientInfo, "Patient info retrieved"));
    }

    /**
     * Get case statistics by status
     */
    @GetMapping("/cases/statistics/by-status")
    @Operation(summary = "Get case statistics by status")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getCaseStatisticsByStatus() {
        log.info("Fetching case statistics by status");
        
        Map<String, Long> statistics = patientAdminService.getCaseStatisticsByStatus();
        
        return ResponseEntity.ok(ApiResponse.success(statistics, "Statistics retrieved"));
    }

    /**
     * Get case statistics by urgency
     */
    @GetMapping("/cases/statistics/by-urgency")
    @Operation(summary = "Get case statistics by urgency level")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getCaseStatisticsByUrgency() {
        log.info("Fetching case statistics by urgency");
        
        Map<String, Long> statistics = patientAdminService.getCaseStatisticsByUrgency();
        
        return ResponseEntity.ok(ApiResponse.success(statistics, "Statistics retrieved"));
    }

    /**
     * Get case statistics by specialization
     */
    @GetMapping("/cases/statistics/by-specialization")
    @Operation(summary = "Get case statistics by specialization")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getCaseStatisticsBySpecialization() {
        log.info("Fetching case statistics by specialization");
        
        Map<String, Long> statistics = patientAdminService.getCaseStatisticsBySpecialization();
        
        return ResponseEntity.ok(ApiResponse.success(statistics, "Statistics retrieved"));
    }
}