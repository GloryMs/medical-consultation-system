package com.supervisorservice.controller;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.coupon.*;
import com.supervisorservice.service.SupervisorCouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller for supervisor coupon management operations.
 * Handles coupon assignment to patients, validation, and retrieval.
 */
@RestController
@RequestMapping("/api/supervisors/coupons")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Supervisor Coupon Management", description = "Endpoints for managing supervisor coupons")
public class SupervisorCouponController {

    private final SupervisorCouponService couponService;

    // ==================== Coupon Assignment ====================

    /**
     * Assign a coupon to a patient
     */
    @PostMapping("/{allocationId}/assign")
    @Operation(summary = "Assign coupon to patient",
               description = "Assign an unassigned coupon to a specific patient")
    public ResponseEntity<ApiResponse<SupervisorCouponAllocationDto>> assignCouponToPatient(
            @PathVariable Long allocationId,
            @Valid @RequestBody AssignCouponToPatientRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("User {} assigning coupon {} to patient {}", userId, allocationId, request.getPatientId());

        SupervisorCouponAllocationDto allocation = couponService.assignCouponToPatient(
                userId, allocationId, request);

        return ResponseEntity.ok(ApiResponse.success(allocation, "Coupon assigned successfully"));
    }

    /**
     * Unassign a coupon from a patient
     */
    @PostMapping("/{allocationId}/unassign")
    @Operation(summary = "Unassign coupon from patient",
               description = "Remove patient assignment from a coupon (make it available again)")
    public ResponseEntity<ApiResponse<SupervisorCouponAllocationDto>> unassignCouponFromPatient(
            @PathVariable Long allocationId,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("User {} unassigning coupon {}", userId, allocationId);

        SupervisorCouponAllocationDto allocation = couponService.unassignCouponFromPatient(userId, allocationId);

        return ResponseEntity.ok(ApiResponse.success(allocation, "Coupon unassigned successfully"));
    }

    // ==================== Coupon Validation ====================

    /**
     * Validate a coupon for use
     */
    @PostMapping("/validate")
    @Operation(summary = "Validate coupon",
               description = "Validate a coupon before using it for payment")
    public ResponseEntity<ApiResponse<CouponValidationResponse>> validateCoupon(
            @RequestParam String couponCode,
            @RequestParam Long patientId,
            @RequestParam Long caseId,
            @RequestParam(required = false) BigDecimal amount,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("User {} validating coupon {} for patient {} case {}",
                userId, couponCode, patientId, caseId);

        CouponValidationResponse response = couponService.validateCoupon(
                userId, couponCode, patientId, caseId, amount != null ? amount : BigDecimal.ZERO);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== Coupon Retrieval ====================

    /**
     * Get all coupons for supervisor
     */
    @GetMapping
    @Operation(summary = "Get all coupons",
               description = "Get all coupon allocations for the supervisor")
    public ResponseEntity<ApiResponse<List<SupervisorCouponAllocationDto>>> getAllCoupons(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("User {} fetching all coupons", userId);

        List<SupervisorCouponAllocationDto> coupons = couponService.getAllCouponsForSupervisor(userId);

        return ResponseEntity.ok(ApiResponse.success(coupons));
    }

    /**
     * Get unassigned (available) coupons
     */
    @GetMapping("/unassigned")
    @Operation(summary = "Get unassigned coupons",
               description = "Get coupons that are not yet assigned to any patient")
    public ResponseEntity<ApiResponse<List<SupervisorCouponAllocationDto>>> getUnassignedCoupons(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("User {} fetching unassigned coupons", userId);

        List<SupervisorCouponAllocationDto> coupons = couponService.getUnassignedCoupons(userId);

        return ResponseEntity.ok(ApiResponse.success(coupons));
    }

    /**
     * Get available coupons for a specific patient
     */
    @GetMapping("/patient/{patientId}/available")
    @Operation(summary = "Get available coupons for patient",
               description = "Get coupons assigned to a patient that are available for use")
    public ResponseEntity<ApiResponse<List<SupervisorCouponAllocationDto>>> getAvailableCouponsForPatient(
            @PathVariable Long patientId,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("User {} fetching available coupons for patient {}", userId, patientId);

        List<SupervisorCouponAllocationDto> coupons = couponService.getAvailableCouponsForPatient(
                userId, patientId);

        return ResponseEntity.ok(ApiResponse.success(coupons));
    }

    /**
     * Get all coupons for a patient (any status)
     */
    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get all coupons for patient",
               description = "Get all coupons assigned to a patient (any status)")
    public ResponseEntity<ApiResponse<List<SupervisorCouponAllocationDto>>> getAllCouponsForPatient(
            @PathVariable Long patientId,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("User {} fetching all coupons for patient {}", userId, patientId);

        List<SupervisorCouponAllocationDto> coupons = couponService.getAllCouponsForPatient(userId, patientId);

        return ResponseEntity.ok(ApiResponse.success(coupons));
    }

    /**
     * Get expiring coupons
     */
    @GetMapping("/expiring")
    @Operation(summary = "Get expiring coupons",
               description = "Get coupons expiring within the specified number of days")
    public ResponseEntity<ApiResponse<List<SupervisorCouponAllocationDto>>> getExpiringCoupons(
            @RequestParam(defaultValue = "30")
            @Parameter(description = "Days until expiration") int days,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("User {} fetching coupons expiring in {} days", userId, days);

        List<SupervisorCouponAllocationDto> coupons = couponService.getExpiringCoupons(userId, days);

        return ResponseEntity.ok(ApiResponse.success(coupons,
                String.format("Found %d coupons expiring within %d days", coupons.size(), days)));
    }

    /**
     * Get coupon by code
     */
    @GetMapping("/code/{couponCode}")
    @Operation(summary = "Get coupon by code",
               description = "Get a specific coupon by its code")
    public ResponseEntity<ApiResponse<SupervisorCouponAllocationDto>> getCouponByCode(
            @PathVariable String couponCode,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("User {} fetching coupon by code {}", userId, couponCode);

        SupervisorCouponAllocationDto coupon = couponService.getCouponByCode(userId, couponCode);

        return ResponseEntity.ok(ApiResponse.success(coupon));
    }

    // ==================== Coupon Summary ====================

    /**
     * Get coupon summary for supervisor
     */
    @GetMapping("/summary")
    @Operation(summary = "Get coupon summary",
               description = "Get coupon statistics for the supervisor")
    public ResponseEntity<ApiResponse<CouponSummaryDto>> getCouponSummary(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("User {} fetching coupon summary", userId);

        CouponSummaryDto summary = couponService.getCouponSummary(userId);

        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * Get coupon summary for a specific patient
     */
    @GetMapping("/summary/patient/{patientId}")
    @Operation(summary = "Get patient coupon summary",
               description = "Get coupon statistics for a specific patient")
    public ResponseEntity<ApiResponse<CouponSummaryDto>> getCouponSummaryForPatient(
            @PathVariable Long patientId,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("User {} fetching coupon summary for patient {}", userId, patientId);

        CouponSummaryDto summary = couponService.getCouponSummaryForPatient(userId, patientId);

        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    // ==================== Sync ====================

    /**
     * Sync coupons with admin service
     */
    @PostMapping("/sync")
    @Operation(summary = "Sync with admin service",
               description = "Synchronize local coupon data with admin service")
    public ResponseEntity<ApiResponse<Void>> syncWithAdminService(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("User {} initiating sync with admin service", userId);

        couponService.syncWithAdminService(userId);

        return ResponseEntity.ok(ApiResponse.success(null, "Sync completed successfully"));
    }
}