package com.paymentservice.controller;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.coupon.CouponValidationResponse;
import com.commonlibrary.entity.BeneficiaryType;
import com.commonlibrary.dto.coupon.CouponPaymentRequestDto;
import com.commonlibrary.dto.coupon.CouponPaymentResponseDto;
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
 * Internal controller for coupon payment operations.
 * Called by other microservices (supervisor-service, patient-service) via Feign.
 */
@RestController
@RequestMapping("/api/payments-internal/coupon")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Internal Coupon Payments", description = "Internal endpoints for coupon payment operations")
public class InternalCouponPaymentController {

    private final CouponPaymentService couponPaymentService;

    /**
     * Validate a coupon for payment (internal use)
     */
    @PostMapping("/validate")
    @Operation(summary = "Validate coupon (internal)",
               description = "Internal endpoint for validating a coupon before payment")
    public ResponseEntity<ApiResponse<CouponValidationResponse>> validateCoupon(
            @RequestParam String couponCode,
            @RequestParam BeneficiaryType beneficiaryType,
            @RequestParam Long beneficiaryId,
            @RequestParam Long patientId,
            @RequestParam Long caseId,
            @RequestParam(required = false) BigDecimal amount) {

        log.info("Internal: Validating coupon {} for {} {} on case {}",
                couponCode, beneficiaryType, beneficiaryId, caseId);

        CouponValidationResponse response = couponPaymentService.validateCoupon(
                couponCode, beneficiaryType, beneficiaryId, patientId, caseId, amount);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Process a coupon payment (internal use)
     */
    @PostMapping("/process")
    @Operation(summary = "Process coupon payment (internal)",
               description = "Internal endpoint for processing a coupon payment")
    public ResponseEntity<ApiResponse<CouponPaymentResponseDto>> processCouponPayment(
            @Valid @RequestBody CouponPaymentRequestDto request,
            @RequestHeader("X-Supervisor-Id") Long supervisorId) {

        log.info("Internal: Processing coupon payment for case {} with coupon {} by user {}",
                request.getCaseId(), request.getCouponCode(), request.getRedeemedByUserId());

        log.info("supervisorId by RequestBody: {}", request.getRedeemedByUserId());
        log.info("supervisorId by RequestHeader: {}", supervisorId);
        request.setRedeemedByUserId(supervisorId);

        CouponPaymentResponseDto response = couponPaymentService.processCouponPayment(request);

        String message = response.isSuccess()
                ? "Payment completed successfully using coupon"
                : "Payment failed";

        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    @Operation(summary = "Health check",
               description = "Check if coupon payment service is healthy")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Coupon payment service is healthy"));
    }
}