package com.adminservice.controller;

import com.adminservice.service.SupervisorManagementService;
import com.commonlibrary.dto.*;
import com.commonlibrary.entity.SupervisorVerificationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for admin operations on medical supervisors
 * Provides endpoints for managing supervisor verification, limits, and monitoring
 */
@RestController
@RequestMapping("/api/admin/supervisors")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Supervisor Management", description = "Admin endpoints for supervisor management and verification")
public class AdminSupervisorController {

    private final SupervisorManagementService supervisorManagementService;

    /**
     * Get all supervisors with optional status filter
     * GET /api/admin/supervisors?status=PENDING
     *
     * @param status Optional verification status filter (PENDING, VERIFIED, REJECTED, SUSPENDED)
     * @return List of supervisor profiles
     */
    @GetMapping
    @Operation(summary = "Get all supervisors",
            description = "Retrieves all supervisors with optional status filter. Admin only.")
    public ResponseEntity<ApiResponse<List<SupervisorProfileDto>>> getAllSupervisors(
            @RequestParam(required = false) SupervisorVerificationStatus status) {

        log.info("GET /api/admin/supervisors - status: {}", status);

        List<SupervisorProfileDto> supervisors = supervisorManagementService.getAllSupervisors(status);

        String message = status != null
                ? String.format("Retrieved %d supervisors with status %s", supervisors.size(), status)
                : String.format("Retrieved %d supervisors", supervisors.size());

        return ResponseEntity.ok(ApiResponse.success(supervisors, message));
    }

    /**
     * Get supervisors pending verification
     * GET /api/admin/supervisors/pending
     *
     * @return List of supervisors pending verification
     */
    @GetMapping("/pending")
    @Operation(summary = "Get pending supervisor verifications",
            description = "Retrieves all supervisors pending verification. Admin only.")
    public ResponseEntity<ApiResponse<List<SupervisorProfileDto>>> getPendingSupervisors() {

        log.info("GET /api/admin/supervisors/pending");

        List<SupervisorProfileDto> supervisors = supervisorManagementService.getPendingSupervisors();

        return ResponseEntity.ok(ApiResponse.success(
                supervisors,
                String.format("Retrieved %d pending supervisors", supervisors.size())));
    }

    /**
     * Get supervisor by ID
     * GET /api/admin/supervisors/{supervisorId}
     *
     * @param supervisorId Supervisor ID
     * @return Supervisor profile details
     */
    @GetMapping("/{supervisorId}")
    @Operation(summary = "Get supervisor by ID",
            description = "Retrieves detailed information about a specific supervisor. Admin only.")
    public ResponseEntity<ApiResponse<SupervisorProfileDto>> getSupervisor(
            @PathVariable Long supervisorId) {

        log.info("GET /api/admin/supervisors/{}", supervisorId);

        SupervisorProfileDto supervisor = supervisorManagementService.getSupervisor(supervisorId);

        return ResponseEntity.ok(ApiResponse.success(supervisor, "Supervisor details retrieved successfully"));
    }

    /**
     * Verify a supervisor
     * PUT /api/admin/supervisors/{supervisorId}/verify
     * Request body:
     * {
     *   "verificationNotes": "All credentials verified and approved"
     * }
     *
     * @param adminUserId Admin user ID from header
     * @param supervisorId Supervisor ID to verify
     * @param request Verification request with notes
     * @return Updated supervisor profile
     */
    @PutMapping("/{supervisorId}/verify")
    @Operation(summary = "Verify supervisor",
            description = "Verifies a supervisor application. Requires admin authentication. Admin only.")
    public ResponseEntity<ApiResponse<SupervisorProfileDto>> verifySupervisor(
            @RequestHeader("X-User-Id") Long adminUserId,
            @PathVariable Long supervisorId,
            @Valid @RequestBody VerifySupervisorRequest request) {

        log.info("PUT /api/admin/supervisors/{}/verify - adminUserId: {}", supervisorId, adminUserId);

        SupervisorProfileDto supervisor = supervisorManagementService.verifySupervisor(
                adminUserId, supervisorId, request);

        return ResponseEntity.ok(ApiResponse.success(supervisor, "Supervisor verified successfully"));
    }

    /**
     * Reject a supervisor
     * PUT /api/admin/supervisors/{supervisorId}/reject
     * Request body:
     * {
     *   "rejectionReason": "Invalid license documentation"
     * }
     *
     * @param supervisorId Supervisor ID to reject
     * @param request Rejection request with reason
     * @return Updated supervisor profile
     */
    @PutMapping("/{supervisorId}/reject")
    @Operation(summary = "Reject supervisor",
            description = "Rejects a supervisor application with reason. Admin only.")
    public ResponseEntity<ApiResponse<SupervisorProfileDto>> rejectSupervisor(
            @PathVariable Long supervisorId,
            @Valid @RequestBody RejectSupervisorRequest request) {

        log.info("PUT /api/admin/supervisors/{}/reject - reason: {}", supervisorId, request.getRejectionReason());

        SupervisorProfileDto supervisor = supervisorManagementService.rejectSupervisor(supervisorId, request);

        return ResponseEntity.ok(ApiResponse.success(supervisor, "Supervisor application rejected"));
    }

    /**
     * Suspend a supervisor
     * PUT /api/admin/supervisors/{supervisorId}/suspend?reason=Violation of terms
     *
     * @param supervisorId Supervisor ID to suspend
     * @param reason Suspension reason
     * @return Updated supervisor profile
     */
    @PutMapping("/{supervisorId}/suspend")
    @Operation(summary = "Suspend supervisor",
            description = "Suspends a supervisor account with reason. Admin only.")
    public ResponseEntity<ApiResponse<SupervisorProfileDto>> suspendSupervisor(
            @PathVariable Long supervisorId,
            @RequestParam String reason) {

        log.info("PUT /api/admin/supervisors/{}/suspend - reason: {}", supervisorId, reason);

        SupervisorProfileDto supervisor = supervisorManagementService.suspendSupervisor(supervisorId, reason);

        return ResponseEntity.ok(ApiResponse.success(supervisor, "Supervisor suspended successfully"));
    }

    /**
     * Update supervisor limits
     * PUT /api/admin/supervisors/{supervisorId}/limits
     * Request body:
     * {
     *   "maxPatientsLimit": 50,
     *   "maxActiveCasesPerPatient": 5
     * }
     *
     * @param supervisorId Supervisor ID
     * @param request Limits update request
     * @return Updated supervisor profile
     */
    @PutMapping("/{supervisorId}/limits")
    @Operation(summary = "Update supervisor limits",
            description = "Updates supervisor patient and case limits. Admin only.")
    public ResponseEntity<ApiResponse<SupervisorProfileDto>> updateLimits(
            @PathVariable Long supervisorId,
            @Valid @RequestBody UpdateSupervisorLimitsRequest request) {

        log.info("PUT /api/admin/supervisors/{}/limits - maxPatients: {}, maxCases: {}",
                supervisorId, request.getMaxPatientsLimit(), request.getMaxActiveCasesPerPatient());

        SupervisorProfileDto supervisor = supervisorManagementService.updateLimits(supervisorId, request);

        return ResponseEntity.ok(ApiResponse.success(supervisor, "Supervisor limits updated successfully"));
    }

    /**
     * Search supervisors
     * GET /api/admin/supervisors/search?query=hospital
     *
     * @param query Search query (searches name, email, organization)
     * @return List of matching supervisors
     */
    @GetMapping("/search")
    @Operation(summary = "Search supervisors",
            description = "Searches supervisors by name, email, or organization. Admin only.")
    public ResponseEntity<ApiResponse<List<SupervisorProfileDto>>> searchSupervisors(
            @RequestParam String query) {

        log.info("GET /api/admin/supervisors/search - query: {}", query);

        List<SupervisorProfileDto> supervisors = supervisorManagementService.searchSupervisors(query);

        return ResponseEntity.ok(ApiResponse.success(
                supervisors,
                String.format("Found %d supervisors matching query: %s", supervisors.size(), query)));
    }

    /**
     * Get supervisor statistics
     * GET /api/admin/supervisors/statistics
     *
     * Returns comprehensive statistics including:
     * - Supervisor counts by status
     * - Patient assignment metrics
     * - Coupon metrics
     * - Payment metrics
     * - Capacity utilization
     * - Recent activity (last 30 days)
     *
     * @return Comprehensive supervisor system statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get supervisor statistics",
            description = "Retrieves comprehensive platform-wide supervisor statistics. Admin only.")
    public ResponseEntity<ApiResponse<SupervisorStatisticsDto>> getStatistics() {

        log.info("GET /api/admin/supervisors/statistics");

        SupervisorStatisticsDto statistics = supervisorManagementService.getStatistics();

        return ResponseEntity.ok(ApiResponse.success(statistics, "Supervisor statistics retrieved successfully"));
    }
}