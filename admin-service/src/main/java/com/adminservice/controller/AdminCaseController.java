package com.adminservice.controller;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.CaseAnalyticsDto;
import com.commonlibrary.dto.CaseDto;
import com.commonlibrary.dto.CaseMetricsDto;
import com.commonlibrary.entity.CaseStatus;
import com.adminservice.dto.AssignCaseRequest;
import com.adminservice.dto.ReassignCaseRequest;
import com.adminservice.dto.CaseFilterDto;
import com.adminservice.service.AdminCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin/cases")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Case Management", description = "Admin endpoints for case management")
//@PreAuthorize("hasRole('ADMIN')")
public class AdminCaseController {

    private final AdminCaseService adminCaseService;


    @GetMapping("/analytics")
    @Operation(summary = "Get case analytics",
            description = "Comprehensive analytics for case management")
    public ResponseEntity<ApiResponse<CaseAnalyticsDto>> getCaseAnalytics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        CaseAnalyticsDto analytics = adminCaseService.getCaseAnalytics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    /**
     * Get all cases with filtering and pagination
     * Supports filters: status, urgencyLevel, specialization, patientId, doctorId, dateRange
     */
    @GetMapping
    @Operation(summary = "Get all cases with filters", 
               description = "Retrieve all cases with optional filtering by status, urgency, specialization, etc.")
    public ResponseEntity<ApiResponse<Page<CaseDto>>> getAllCases(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String urgencyLevel,
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String searchTerm,
            Pageable pageable) {
        
        log.info("Admin fetching cases with filters - status: {}, urgency: {}, specialization: {}", 
                 status, urgencyLevel, specialization);
        
        CaseFilterDto filterDto = CaseFilterDto.builder()
                .status(status)
                .urgencyLevel(urgencyLevel)
                .specialization(specialization)
                .patientId(patientId)
                .doctorId(doctorId)
                .startDate(startDate)
                .endDate(endDate)
                .searchTerm(searchTerm)
                .build();
        
        Page<CaseDto> cases = adminCaseService.getAllCases(filterDto, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(cases, 
                String.format("Retrieved %d cases", cases.getTotalElements())));
    }

    /**
     * Get case by ID with full details
     */
    @GetMapping("/{caseId}")
    @Operation(summary = "Get case details", 
               description = "Retrieve complete case details including patient info, assignments, and history")
    public ResponseEntity<ApiResponse<CaseDto>> getCaseById(@PathVariable Long caseId) {
        log.info("Admin fetching case details for caseId: {}", caseId);
        
        CaseDto caseDto = adminCaseService.getCaseById(caseId);
        
        return ResponseEntity.ok(ApiResponse.success(caseDto, "Case details retrieved successfully"));
    }

    /**
     * Assign case to doctor
     * Validates doctor availability and specialization match
     * Creates case assignment record
     * Updates case status to ASSIGNED
     * Sends notification to doctor
     */
    @PostMapping("/{caseId}/assign")
    @Operation(summary = "Assign case to doctor", 
               description = "Manually assign a case to a specific doctor. Validates doctor specialization and availability.")
    public ResponseEntity<ApiResponse<Void>> assignCaseToDoctor(
            @PathVariable Long caseId,
            @Valid @RequestBody AssignCaseRequest request,
            @RequestHeader("X-User-Id") Long adminUserId) {
        
        log.info("Admin {} assigning case {} to doctor {}", adminUserId, caseId, request.getDoctorId());
        
        adminCaseService.assignCaseToDoctor(caseId, request.getDoctorId(), adminUserId);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Case assigned successfully"));
    }

    /**
     * Reassign case to different doctor
     * Records reassignment reason
     * Updates previous assignment status
     * Creates new assignment
     * Notifies both doctors
     */
    @PostMapping("/{caseId}/reassign")
    @Operation(summary = "Reassign case to another doctor", 
               description = "Reassign an already assigned case to a different doctor with reason documentation")
    public ResponseEntity<ApiResponse<Void>> reassignCase(
            @PathVariable Long caseId,
            @Valid @RequestBody ReassignCaseRequest request,
            @RequestHeader("X-User-Id") Long adminUserId) {
        
        log.info("Admin {} reassigning case {} from current doctor to doctor {} - Reason: {}", 
                 adminUserId, caseId, request.getNewDoctorId(), request.getReason());
        
        adminCaseService.reassignCase(caseId, request.getNewDoctorId(), request.getReason(), adminUserId);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Case reassigned successfully"));
    }

    /**
     * Update case status (admin override)
     * Allows admin to manually change case status
     * Records admin action in audit log
     */
    @PutMapping("/{caseId}/status")
    @Operation(summary = "Update case status", 
               description = "Admin override to manually update case status with reason")
    public ResponseEntity<ApiResponse<Void>> updateCaseStatus(
            @PathVariable Long caseId,
            @RequestParam CaseStatus newStatus,
            @RequestParam(required = false) String reason,
            @RequestHeader("X-User-Id") Long adminUserId) {
        
        log.info("Admin {} updating case {} status to {} - Reason: {}", 
                 adminUserId, caseId, newStatus, reason);
        
        adminCaseService.updateCaseStatus(caseId, newStatus, reason, adminUserId);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Case status updated successfully"));
    }

    /**
     * Get case metrics and statistics
     * Returns comprehensive case statistics for admin dashboard:
     * - Total cases by status
     * - Cases by urgency level
     * - Average resolution time
     * - Assignment efficiency
     * - Status distribution
     */
    @GetMapping("/metrics")
    @Operation(summary = "Get case metrics", 
               description = "Retrieve comprehensive case statistics and metrics for admin dashboard")
    public ResponseEntity<ApiResponse<CaseMetricsDto>> getCaseMetrics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        log.info("Admin fetching case metrics from {} to {}", startDate, endDate);
        
        CaseMetricsDto metrics = adminCaseService.getCaseMetrics(startDate, endDate);
        
        return ResponseEntity.ok(ApiResponse.success(metrics, "Metrics retrieved successfully"));
    }

    /**
     * Get case assignment history
     */
    @GetMapping("/{caseId}/assignments")
    @Operation(summary = "Get case assignment history", 
               description = "Retrieve all assignment attempts and changes for a case")
    public ResponseEntity<ApiResponse<List<?>>> getCaseAssignmentHistory(@PathVariable Long caseId) {
        log.info("Admin fetching assignment history for case {}", caseId);
        
        List<?> assignmentHistory = adminCaseService.getCaseAssignmentHistory(caseId);
        
        return ResponseEntity.ok(ApiResponse.success(assignmentHistory, 
                "Assignment history retrieved successfully"));
    }

    /**
     * Get unassigned cases (pending assignment)
     */
    @GetMapping("/unassigned")
    @Operation(summary = "Get unassigned cases", 
               description = "Retrieve all cases pending doctor assignment")
    public ResponseEntity<ApiResponse<List<CaseDto>>> getUnassignedCases(
            @RequestParam(required = false) String urgencyLevel,
            @RequestParam(required = false) String specialization) {
        
        log.info("Admin fetching unassigned cases - urgency: {}, specialization: {}", 
                 urgencyLevel, specialization);
        
        List<CaseDto> cases = adminCaseService.getUnassignedCases(urgencyLevel, specialization);
        
        return ResponseEntity.ok(ApiResponse.success(cases, 
                String.format("Retrieved %d unassigned cases", cases.size())));
    }

    /**
     * Get overdue cases (past expected completion date)
     */
    @GetMapping("/overdue")
    @Operation(summary = "Get overdue cases", 
               description = "Retrieve cases that are past their expected completion date")
    public ResponseEntity<ApiResponse<List<CaseDto>>> getOverdueCases() {
        log.info("Admin fetching overdue cases");
        
        List<CaseDto> cases = adminCaseService.getOverdueCases();
        
        return ResponseEntity.ok(ApiResponse.success(cases, 
                String.format("Retrieved %d overdue cases", cases.size())));
    }

    /**
     * Bulk case assignment
     * Allows admin to assign multiple cases to doctors in one operation
     */
    @PostMapping("/bulk-assign")
    @Operation(summary = "Bulk assign cases", 
               description = "Assign multiple cases to doctors in a single operation")
    public ResponseEntity<ApiResponse<Void>> bulkAssignCases(
            @Valid @RequestBody List<AssignCaseRequest> assignments,
            @RequestHeader("X-User-Id") Long adminUserId) {
        
        log.info("Admin {} performing bulk assignment for {} cases", adminUserId, assignments.size());
        
        adminCaseService.bulkAssignCases(assignments, adminUserId);
        
        return ResponseEntity.ok(ApiResponse.success(null, 
                String.format("Successfully assigned %d cases", assignments.size())));
    }

    /**
     * Export cases to CSV/Excel
     */
    @GetMapping("/export")
    @Operation(summary = "Export cases", 
               description = "Export cases data in CSV or Excel format")
    public ResponseEntity<?> exportCases(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        log.info("Admin exporting cases - format: {}, status: {}", format, status);
        
        // Implementation would return file download response
        // This is a placeholder for the actual export implementation
        
        return ResponseEntity.ok(ApiResponse.success(null, "Export initiated"));
    }
}