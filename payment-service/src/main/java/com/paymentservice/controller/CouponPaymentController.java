package com.paymentservice.controller;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.coupon.AdminCouponDto;
import com.commonlibrary.dto.coupon.CouponValidationResponse;
import com.commonlibrary.entity.BeneficiaryType;
import com.paymentservice.dto.CouponPaymentRequestDto;
import com.paymentservice.dto.CouponPaymentResponseDto;
import com.paymentservice.service.CouponPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Controller for coupon-based payment operations.
 * Provides endpoints for validating and processing coupon payments.
 */
@RestController
@RequestMapping("/api/payments/coupon")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Coupon Payments", description = "Endpoints for coupon-based payment operations")
public class CouponPaymentController {

    private final CouponPaymentService couponPaymentService;

    /**
     * Validate a coupon for payment
     */
    @PostMapping("/validate")
    @Operation(summary = "Validate coupon",
               description = "Validate a coupon before using it for payment")
    public ResponseEntity<ApiResponse<CouponValidationResponse>> validateCoupon(
            @RequestParam String couponCode,
            @RequestParam BeneficiaryType beneficiaryType,
            @RequestParam Long beneficiaryId,
            @RequestParam Long patientId,
            @RequestParam Long caseId,
            @RequestParam(required = false) BigDecimal amount) {

        log.info("Validating coupon {} for {} {} on case {}",
                couponCode, beneficiaryType, beneficiaryId, caseId);

        CouponValidationResponse response = couponPaymentService.validateCoupon(
                couponCode, beneficiaryType, beneficiaryId, patientId, caseId, amount);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Process a coupon payment
     */
    @PostMapping("/process")
    @Operation(summary = "Process coupon payment",
               description = "Process a payment using a coupon")
    public ResponseEntity<ApiResponse<CouponPaymentResponseDto>> processCouponPayment(
            @Valid @RequestBody CouponPaymentRequestDto request,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Processing coupon payment for case {} with coupon {} by user {}",
                request.getCaseId(), request.getCouponCode(), userId);

        // Set the redeemer user ID
        request.setRedeemedByUserId(userId);

        CouponPaymentResponseDto response = couponPaymentService.processCouponPayment(request);

        String message = response.isSuccess() 
                ? "Payment completed successfully using coupon" 
                : "Payment failed";

        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    /**
     * Get coupon details
     */
    @GetMapping("/{couponCode}")
    @Operation(summary = "Get coupon details",
               description = "Get details of a specific coupon")
    public ResponseEntity<ApiResponse<AdminCouponDto>> getCouponDetails(
            @PathVariable String couponCode) {

        log.info("Fetching coupon details for {}", couponCode);

        AdminCouponDto coupon = couponPaymentService.getCouponDetails(couponCode);

        if (coupon != null) {
            return ResponseEntity.ok(ApiResponse.success(coupon));
        } else {
            return ResponseEntity.ok(ApiResponse.error("Coupon not found", 
                    org.springframework.http.HttpStatus.NOT_FOUND));
        }
    }
}