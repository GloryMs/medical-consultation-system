package com.paymentservice.feign;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.coupon.*;
import com.commonlibrary.entity.BeneficiaryType;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Feign client for admin-service coupon operations.
 * Used by payment-service to validate and mark coupons as used.
 */
@FeignClient(name = "admin-service")
public interface AdminCouponClient {

    // ==================== Validation ====================

    /**
     * Validate a coupon for redemption.
     * Called before processing payment to ensure coupon is valid.
     */
    @PostMapping("/validate")
    ResponseEntity<ApiResponse<CouponValidationResponse>> validateCoupon(
            @RequestBody CouponValidationRequest request);

    /**
     * Mark a coupon as used after successful payment.
     * Called after payment is processed to update coupon status.
     */
    @PostMapping("/{couponCode}/mark-used")
    ResponseEntity<ApiResponse<MarkCouponUsedResponse>> markCouponAsUsed(
            @PathVariable("couponCode") String couponCode,
            @RequestBody MarkCouponUsedRequest request);

    // ==================== Coupon Lookup ====================

    /**
     * Get coupon by code.
     */
    @GetMapping("/code/{couponCode}")
    ResponseEntity<ApiResponse<AdminCouponDto>> getCouponByCode(
            @PathVariable("couponCode") String couponCode);

    /**
     * Get available coupons for a beneficiary.
     */
    @GetMapping("/beneficiary/{type}/{beneficiaryId}/available")
    ResponseEntity<ApiResponse<List<AdminCouponDto>>> getAvailableCouponsForBeneficiary(
            @PathVariable("type") BeneficiaryType type,
            @PathVariable("beneficiaryId") Long beneficiaryId);

    // ==================== Summary ====================

    /**
     * Get coupon summary for a beneficiary.
     */
    @GetMapping("/summary/beneficiary/{type}/{beneficiaryId}")
    ResponseEntity<ApiResponse<CouponSummaryDto>> getCouponSummaryForBeneficiary(
            @PathVariable("type") BeneficiaryType type,
            @PathVariable("beneficiaryId") Long beneficiaryId);

    // ==================== Health Check ====================

    /**
     * Health check for coupon service.
     */
    @GetMapping("/health")
    ResponseEntity<ApiResponse<String>> healthCheck();
}