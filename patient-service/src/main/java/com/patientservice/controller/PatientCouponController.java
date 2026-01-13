package com.patientservice.controller;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.PatientCouponAllocationDto;
import com.commonlibrary.dto.coupon.CouponSummaryDto;
import com.commonlibrary.dto.coupon.CouponValidationResponse;
import com.patientservice.service.PatientCouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller for patient coupon operations.
 * Allows patients to view and validate their available coupons.
 */
@RestController
@RequestMapping("/api/patients/coupons")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Patient Coupons", description = "Endpoints for patient coupon operations")
public class PatientCouponController {

    private final PatientCouponService couponService;

    /**
     * Get available coupons for the patient
     */
    @GetMapping("/available")
    @Operation(summary = "Get available coupons",
               description = "Get coupons that are available for use by the patient")
    public ResponseEntity<ApiResponse<List<PatientCouponAllocationDto>>> getAvailableCoupons(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Patient {} fetching available coupons", userId);

        // Note: In a real implementation, you'd get patientId from userId
        // For now, assuming userId == patientId for direct patients
        List<PatientCouponAllocationDto> coupons = couponService.getAvailableCoupons(userId);

        return ResponseEntity.ok(ApiResponse.success(coupons));
    }

    /**
     * Get all coupons for the patient (any status)
     */
    @GetMapping
    @Operation(summary = "Get all coupons",
               description = "Get all coupons for the patient (any status)")
    public ResponseEntity<ApiResponse<List<PatientCouponAllocationDto>>> getAllCoupons(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Patient {} fetching all coupons", userId);

        List<PatientCouponAllocationDto> coupons = couponService.getAllCoupons(userId);

        return ResponseEntity.ok(ApiResponse.success(coupons));
    }

    /**
     * Get coupon summary for the patient
     */
    @GetMapping("/summary")
    @Operation(summary = "Get coupon summary",
               description = "Get coupon statistics for the patient")
    public ResponseEntity<ApiResponse<CouponSummaryDto>> getCouponSummary(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Patient {} fetching coupon summary", userId);

        CouponSummaryDto summary = couponService.getCouponSummary(userId);

        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * Validate a coupon for use
     */
    @PostMapping("/validate")
    @Operation(summary = "Validate coupon",
               description = "Validate a coupon before using it for payment")
    public ResponseEntity<ApiResponse<CouponValidationResponse>> validateCoupon(
            @RequestParam String couponCode,
            @RequestParam Long caseId,
            @RequestParam(required = false) BigDecimal amount,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Patient {} validating coupon {} for case {}", userId, couponCode, caseId);

        CouponValidationResponse response = couponService.validateCoupon(
                userId, couponCode, caseId, amount);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get coupon by code
     */
    @GetMapping("/code/{couponCode}")
    @Operation(summary = "Get coupon by code",
               description = "Get details of a specific coupon by its code")
    public ResponseEntity<ApiResponse<PatientCouponAllocationDto>> getCouponByCode(
            @PathVariable String couponCode,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Patient {} fetching coupon by code {}", userId, couponCode);

        PatientCouponAllocationDto coupon = couponService.getCouponByCode(couponCode);

        if (coupon != null) {
            return ResponseEntity.ok(ApiResponse.success(coupon));
        } else {
            return ResponseEntity.ok(ApiResponse.error("Coupon not found", 
                    org.springframework.http.HttpStatus.NOT_FOUND));
        }
    }
}