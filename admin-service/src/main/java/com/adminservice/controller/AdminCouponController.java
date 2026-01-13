package com.adminservice.controller;

import com.adminservice.service.AdminCouponService;
import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.coupon.*;
import com.commonlibrary.entity.AdminCouponStatus;
import com.commonlibrary.entity.BeneficiaryType;
import com.commonlibrary.entity.CouponBatchStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for coupon management operations.
 * Provides endpoints for creating, distributing, validating, and managing coupons.
 */
@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Coupon Management", description = "Endpoints for managing coupons")
public class AdminCouponController {

    private final AdminCouponService couponService;

    // ==================== Coupon Creation ====================

    /**
     * Create a single coupon
     */
    @PostMapping
    @Operation(summary = "Create a single coupon", 
               description = "Create a new coupon with specified discount configuration")
    public ResponseEntity<ApiResponse<AdminCouponDto>> createCoupon(
            @Valid @RequestBody CreateCouponRequest request,
            @RequestHeader("X-User-Id") Long adminUserId) {
        
        log.info("Admin {} creating coupon", adminUserId);
        
        AdminCouponDto coupon = couponService.createCoupon(request, adminUserId);
        
        return ResponseEntity.ok(ApiResponse.success(coupon, "Coupon created successfully"));
    }

    /**
     * Create a batch of coupons
     */
    @PostMapping("/batch")
    @Operation(summary = "Create batch coupons", 
               description = "Create multiple coupons at once with the same configuration")
    public ResponseEntity<ApiResponse<CouponBatchDto>> createBatchCoupons(
            @Valid @RequestBody CreateBatchCouponsRequest request,
            @RequestHeader("X-User-Id") Long adminUserId) {
        
        log.info("Admin {} creating batch of {} coupons", adminUserId, request.getTotalCoupons());
        
        CouponBatchDto batch = couponService.createBatchCoupons(request, adminUserId);
        
        return ResponseEntity.ok(ApiResponse.success(batch, 
                String.format("Batch created with %d coupons", request.getTotalCoupons())));
    }

    // ==================== Coupon Distribution ====================

    /**
     * Distribute a single coupon to a beneficiary
     */
    @PostMapping("/{couponId}/distribute")
    @Operation(summary = "Distribute coupon", 
               description = "Distribute a coupon to a supervisor or patient")
    public ResponseEntity<ApiResponse<AdminCouponDto>> distributeCoupon(
            @PathVariable Long couponId,
            @Valid @RequestBody DistributeCouponRequest request,
            @RequestHeader("X-User-Id") Long adminUserId) {
        
        log.info("Admin {} distributing coupon {} to {} {}", 
                adminUserId, couponId, request.getBeneficiaryType(), request.getBeneficiaryId());
        
        AdminCouponDto coupon = couponService.distributeCoupon(couponId, request, adminUserId);
        
        return ResponseEntity.ok(ApiResponse.success(coupon, "Coupon distributed successfully"));
    }

    /**
     * Distribute an entire batch to a beneficiary
     */
    @PostMapping("/batch/{batchId}/distribute")
    @Operation(summary = "Distribute batch", 
               description = "Distribute all coupons in a batch to a supervisor or patient")
    public ResponseEntity<ApiResponse<CouponBatchDto>> distributeBatch(
            @PathVariable Long batchId,
            @Valid @RequestBody DistributeCouponRequest request,
            @RequestHeader("X-User-Id") Long adminUserId) {
        
        log.info("Admin {} distributing batch {} to {} {}", 
                adminUserId, batchId, request.getBeneficiaryType(), request.getBeneficiaryId());
        
        CouponBatchDto batch = couponService.distributeBatch(batchId, request, adminUserId);
        
        return ResponseEntity.ok(ApiResponse.success(batch, "Batch distributed successfully"));
    }

    // ==================== Coupon Validation (Internal API) ====================

    /**
     * Validate a coupon for redemption
     * Called by payment-service before processing payment
     */
    @PostMapping("/validate")
    @Operation(summary = "Validate coupon", 
               description = "Validate a coupon for redemption (called by payment-service)")
    public ResponseEntity<ApiResponse<CouponValidationResponse>> validateCoupon(
            @Valid @RequestBody CouponValidationRequest request) {
        
        log.info("Validating coupon {} for beneficiary {} {}", 
                request.getCouponCode(), request.getBeneficiaryType(), request.getBeneficiaryId());
        
        CouponValidationResponse response = couponService.validateCoupon(request);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== Mark Coupon as Used (Internal API) ====================

    /**
     * Mark a coupon as used
     * Called by payment-service after successful payment
     */
    @PostMapping("/{couponCode}/mark-used")
    @Operation(summary = "Mark coupon as used", 
               description = "Mark a coupon as used after successful payment (called by payment-service)")
    public ResponseEntity<ApiResponse<MarkCouponUsedResponse>> markCouponAsUsed(
            @PathVariable String couponCode,
            @Valid @RequestBody MarkCouponUsedRequest request) {
        
        log.info("Marking coupon {} as used for case {} payment {}", 
                couponCode, request.getCaseId(), request.getPaymentId());
        
        MarkCouponUsedResponse response = couponService.markCouponAsUsed(couponCode, request);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== Coupon Cancellation ====================

    /**
     * Cancel a single coupon
     */
    @PostMapping("/{couponId}/cancel")
    @Operation(summary = "Cancel coupon", 
               description = "Cancel a coupon (cannot cancel used coupons)")
    public ResponseEntity<ApiResponse<AdminCouponDto>> cancelCoupon(
            @PathVariable Long couponId,
            @Valid @RequestBody CancelCouponRequest request,
            @RequestHeader("X-User-Id") Long adminUserId) {
        
        log.info("Admin {} cancelling coupon {}", adminUserId, couponId);
        
        AdminCouponDto coupon = couponService.cancelCoupon(couponId, request, adminUserId);
        
        return ResponseEntity.ok(ApiResponse.success(coupon, "Coupon cancelled successfully"));
    }

    /**
     * Cancel all coupons in a batch
     */
    @PostMapping("/batch/{batchId}/cancel")
    @Operation(summary = "Cancel batch", 
               description = "Cancel all non-used coupons in a batch")
    public ResponseEntity<ApiResponse<CouponBatchDto>> cancelBatch(
            @PathVariable Long batchId,
            @Valid @RequestBody CancelCouponRequest request,
            @RequestHeader("X-User-Id") Long adminUserId) {
        
        log.info("Admin {} cancelling batch {}", adminUserId, batchId);
        
        CouponBatchDto batch = couponService.cancelBatch(batchId, request, adminUserId);
        
        return ResponseEntity.ok(ApiResponse.success(batch, "Batch cancelled successfully"));
    }

    // ==================== Coupon Retrieval ====================

    /**
     * Get coupon by ID
     */
    @GetMapping("/{couponId}")
    @Operation(summary = "Get coupon by ID")
    public ResponseEntity<ApiResponse<AdminCouponDto>> getCouponById(@PathVariable Long couponId) {
        AdminCouponDto coupon = couponService.getCouponById(couponId);
        return ResponseEntity.ok(ApiResponse.success(coupon));
    }

    /**
     * Get coupon by code
     */
    @GetMapping("/code/{couponCode}")
    @Operation(summary = "Get coupon by code")
    public ResponseEntity<ApiResponse<AdminCouponDto>> getCouponByCode(@PathVariable String couponCode) {
        AdminCouponDto coupon = couponService.getCouponByCode(couponCode);
        return ResponseEntity.ok(ApiResponse.success(coupon));
    }

    /**
     * Get all coupons with filters and pagination
     */
    @GetMapping
    @Operation(summary = "Get all coupons", 
               description = "Get coupons with optional filters and pagination")
    public ResponseEntity<ApiResponse<Page<AdminCouponDto>>> getCoupons(
            @RequestParam(required = false) AdminCouponStatus status,
            @RequestParam(required = false) BeneficiaryType beneficiaryType,
            @RequestParam(required = false) Long beneficiaryId,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) String couponCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<AdminCouponDto> coupons = couponService.getCoupons(
                status, beneficiaryType, beneficiaryId, batchId, couponCode, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(coupons));
    }

    /**
     * Get coupons for a specific beneficiary
     */
    @GetMapping("/beneficiary/{type}/{beneficiaryId}")
    @Operation(summary = "Get coupons for beneficiary", 
               description = "Get all coupons for a specific supervisor or patient")
    public ResponseEntity<ApiResponse<List<AdminCouponDto>>> getCouponsForBeneficiary(
            @PathVariable BeneficiaryType type,
            @PathVariable Long beneficiaryId) {
        
        List<AdminCouponDto> coupons = couponService.getCouponsForBeneficiary(type, beneficiaryId);
        return ResponseEntity.ok(ApiResponse.success(coupons));
    }

    /**
     * Get available coupons for a beneficiary
     */
    @GetMapping("/beneficiary/{type}/{beneficiaryId}/available")
    @Operation(summary = "Get available coupons for beneficiary", 
               description = "Get available (not used, not expired) coupons for a beneficiary")
    public ResponseEntity<ApiResponse<List<AdminCouponDto>>> getAvailableCouponsForBeneficiary(
            @PathVariable BeneficiaryType type,
            @PathVariable Long beneficiaryId) {
        
        List<AdminCouponDto> coupons = couponService.getAvailableCouponsForBeneficiary(type, beneficiaryId);
        return ResponseEntity.ok(ApiResponse.success(coupons));
    }

    /**
     * Get expiring coupons
     */
    @GetMapping("/expiring")
    @Operation(summary = "Get expiring coupons", 
               description = "Get coupons expiring within specified days")
    public ResponseEntity<ApiResponse<List<AdminCouponDto>>> getExpiringCoupons(
            @RequestParam(defaultValue = "30") 
            @Parameter(description = "Days until expiration") int days) {
        
        List<AdminCouponDto> coupons = couponService.getExpiringCoupons(days);
        return ResponseEntity.ok(ApiResponse.success(coupons, 
                String.format("Found %d coupons expiring within %d days", coupons.size(), days)));
    }

    // ==================== Batch Retrieval ====================

    /**
     * Get batch by ID
     */
    @GetMapping("/batch/{batchId}")
    @Operation(summary = "Get batch by ID")
    public ResponseEntity<ApiResponse<CouponBatchDto>> getBatchById(@PathVariable Long batchId) {
        CouponBatchDto batch = couponService.getBatchById(batchId);
        return ResponseEntity.ok(ApiResponse.success(batch));
    }

    /**
     * Get all batches with filters and pagination
     */
    @GetMapping("/batches")
    @Operation(summary = "Get all batches", 
               description = "Get batches with optional filters and pagination")
    public ResponseEntity<ApiResponse<Page<CouponBatchDto>>> getBatches(
            @RequestParam(required = false) CouponBatchStatus status,
            @RequestParam(required = false) BeneficiaryType beneficiaryType,
            @RequestParam(required = false) Long beneficiaryId,
            @RequestParam(required = false) String batchCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<CouponBatchDto> batches = couponService.getBatches(
                status, beneficiaryType, beneficiaryId, batchCode, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(batches));
    }

    /**
     * Get coupons in a batch
     */
    @GetMapping("/batch/{batchId}/coupons")
    @Operation(summary = "Get coupons in batch", 
               description = "Get all coupons belonging to a specific batch")
    public ResponseEntity<ApiResponse<Page<AdminCouponDto>>> getCouponsInBatch(
            @PathVariable Long batchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AdminCouponDto> coupons = couponService.getCoupons(
                null, null, null, batchId, null, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(coupons));
    }

    // ==================== Statistics & Analytics ====================

    /**
     * Get overall coupon statistics
     */
    @GetMapping("/summary")
    @Operation(summary = "Get coupon summary", 
               description = "Get overall coupon statistics and counts")
    public ResponseEntity<ApiResponse<CouponSummaryDto>> getCouponSummary() {
        CouponSummaryDto summary = couponService.getCouponSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * Get coupon summary for a specific beneficiary
     */
    @GetMapping("/summary/beneficiary/{type}/{beneficiaryId}")
    @Operation(summary = "Get beneficiary coupon summary", 
               description = "Get coupon statistics for a specific supervisor or patient")
    public ResponseEntity<ApiResponse<CouponSummaryDto>> getCouponSummaryForBeneficiary(
            @PathVariable BeneficiaryType type,
            @PathVariable Long beneficiaryId) {
        
        CouponSummaryDto summary = couponService.getCouponSummaryForBeneficiary(type, beneficiaryId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}