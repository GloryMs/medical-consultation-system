package com.adminservice.controller;

import com.adminservice.service.AdminCouponService;
import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.coupon.*;
import com.commonlibrary.entity.BeneficiaryType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Internal REST Controller for coupon operations called by other microservices.
 * These endpoints are used by payment-service, supervisor-service, and patient-service
 * via Feign clients.
 */
@RestController
@RequestMapping("/api/internal/coupons")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Internal Coupon API", description = "Internal endpoints for inter-service coupon operations")
public class InternalCouponController {

    private final AdminCouponService couponService;

    // ==================== Validation Endpoints (Called by Payment-Service) ====================

    /**
     * Validate a coupon for redemption
     * Called by payment-service before processing payment
     */
    @PostMapping("/validate")
    @Operation(summary = "Validate coupon for redemption",
               description = "Validates a coupon can be used for payment. Called by payment-service.")
    public ResponseEntity<ApiResponse<CouponValidationResponse>> validateCoupon(
            @Valid @RequestBody CouponValidationRequest request) {

        log.info("Internal API: Validating coupon {} for beneficiary {} {}",
                request.getCouponCode(), request.getBeneficiaryType(), request.getBeneficiaryId());

        CouponValidationResponse response = couponService.validateCoupon(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Mark a coupon as used after successful payment
     * Called by payment-service after payment is processed
     */
    @PostMapping("/{couponCode}/mark-used")
    @Operation(summary = "Mark coupon as used",
               description = "Marks a coupon as used after successful payment. Called by payment-service.")
    public ResponseEntity<ApiResponse<MarkCouponUsedResponse>> markCouponAsUsed(
            @PathVariable String couponCode,
            @Valid @RequestBody MarkCouponUsedRequest request) {

        log.info("Internal API: Marking coupon {} as used for case {} payment {}",
                couponCode, request.getCaseId(), request.getPaymentId());

        MarkCouponUsedResponse response = couponService.markCouponAsUsed(couponCode, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== Lookup Endpoints (Called by Supervisor-Service) ====================

    /**
     * Get coupon by code
     * Called by supervisor-service for coupon lookup
     */
    @GetMapping("/code/{couponCode}")
    @Operation(summary = "Get coupon by code",
               description = "Retrieve coupon details by code. Called by supervisor-service.")
    public ResponseEntity<ApiResponse<AdminCouponDto>> getCouponByCode(
            @PathVariable String couponCode) {

        log.info("Internal API: Looking up coupon by code: {}", couponCode);

        AdminCouponDto coupon = couponService.getCouponByCode(couponCode);

        return ResponseEntity.ok(ApiResponse.success(coupon));
    }

    /**
     * Get available coupons for a beneficiary
     * Called by supervisor-service or patient-service
     */
    @GetMapping("/beneficiary/{type}/{beneficiaryId}/available")
    @Operation(summary = "Get available coupons for beneficiary",
               description = "Retrieve available coupons for a supervisor or patient.")
    public ResponseEntity<ApiResponse<List<AdminCouponDto>>> getAvailableCouponsForBeneficiary(
            @PathVariable BeneficiaryType type,
            @PathVariable Long beneficiaryId) {

        log.info("Internal API: Getting available coupons for {} {}", type, beneficiaryId);

        List<AdminCouponDto> coupons = couponService.getAvailableCouponsForBeneficiary(type, beneficiaryId);

        return ResponseEntity.ok(ApiResponse.success(coupons));
    }

    /**
     * Get all coupons for a beneficiary
     * Called by supervisor-service or patient-service
     */
    @GetMapping("/beneficiary/{type}/{beneficiaryId}")
    @Operation(summary = "Get all coupons for beneficiary",
               description = "Retrieve all coupons (any status) for a supervisor or patient.")
    public ResponseEntity<ApiResponse<List<AdminCouponDto>>> getCouponsForBeneficiary(
            @PathVariable BeneficiaryType type,
            @PathVariable Long beneficiaryId) {

        log.info("Internal API: Getting all coupons for {} {}", type, beneficiaryId);

        List<AdminCouponDto> coupons = couponService.getCouponsForBeneficiary(type, beneficiaryId);

        return ResponseEntity.ok(ApiResponse.success(coupons));
    }

    /**
     * Get coupon summary for a beneficiary
     * Called by supervisor-service for dashboard
     */
    @GetMapping("/summary/beneficiary/{type}/{beneficiaryId}")
    @Operation(summary = "Get coupon summary for beneficiary",
               description = "Retrieve coupon statistics for a supervisor or patient.")
    public ResponseEntity<ApiResponse<CouponSummaryDto>> getCouponSummaryForBeneficiary(
            @PathVariable BeneficiaryType type,
            @PathVariable Long beneficiaryId) {

        log.info("Internal API: Getting coupon summary for {} {}", type, beneficiaryId);

        CouponSummaryDto summary = couponService.getCouponSummaryForBeneficiary(type, beneficiaryId);

        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * Get expiring coupons for a beneficiary
     * Called by supervisor-service for warnings
     */
    @GetMapping("/beneficiary/{type}/{beneficiaryId}/expiring")
    @Operation(summary = "Get expiring coupons for beneficiary",
               description = "Retrieve coupons expiring soon for a supervisor or patient.")
    public ResponseEntity<ApiResponse<List<AdminCouponDto>>> getExpiringCouponsForBeneficiary(
            @PathVariable BeneficiaryType type,
            @PathVariable Long beneficiaryId,
            @RequestParam(defaultValue = "30") int days) {

        log.info("Internal API: Getting expiring coupons (within {} days) for {} {}",
                days, type, beneficiaryId);

        // Get all expiring coupons and filter by beneficiary
        List<AdminCouponDto> allExpiring = couponService.getExpiringCoupons(days);
        List<AdminCouponDto> beneficiaryExpiring = allExpiring.stream()
                .filter(c -> c.getBeneficiaryType() == type && 
                            c.getBeneficiaryId().equals(beneficiaryId))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(beneficiaryExpiring));
    }

    // ==================== Health Check ====================

    /**
     * Health check endpoint for Feign clients
     */
    @GetMapping("/health")
    @Operation(summary = "Health check",
               description = "Simple health check for coupon service availability.")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Coupon service is healthy"));
    }
}