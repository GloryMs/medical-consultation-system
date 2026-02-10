package com.supervisorservice.controller;

import com.commonlibrary.dto.PaymentHistoryDto;
import com.commonlibrary.dto.coupon.CouponValidationResponse;
import com.commonlibrary.dto.coupon.SupervisorCouponAllocationDto;
import com.supervisorservice.dto.*;
import com.supervisorservice.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST controller for supervisor payment and coupon operations
 */
@RestController
@RequestMapping("/api/supervisors/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Supervisor Payments", description = "Payment and coupon management endpoints")
public class SupervisorPaymentController {
    
    //private final CouponService couponService;
    private final AppointmentManagementService appointmentService;
    //private final PaymentManagementService paymentManagementService;
    private final SupervisorPaymentService paymentService;
    private final SupervisorCouponService couponService;

    
//    /**
//     * Redeem coupon for case payment
//     */
//    @PostMapping("/coupon/{caseId}")
//    @Operation(summary = "Redeem coupon",
//               description = "Redeems a coupon to pay for a case consultation")
//    public ResponseEntity<ApiResponse<PaymentResultDto>> redeemCoupon(
//            @RequestHeader("X-User-Id") Long userId,
//            @PathVariable Long caseId,
//            @RequestParam Long patientId,
//            @Valid @RequestBody RedeemCouponRequest request) {
//
//        log.info("POST /api/supervisors/payments/coupon/{} - userId: {}, couponCode: {}",
//                caseId, userId, request.getCouponCode());
//
//        PaymentResultDto result = couponService.redeemCoupon(
//                userId, caseId, request.getCouponCode(), patientId);
//
//        return ResponseEntity.ok(ApiResponse.success("Coupon redeemed successfully", result));
//    }

//    /**
//     * Get coupon summary
//     */
//    @GetMapping("/coupons")
//    @Operation(summary = "Get coupon summary",
//               description = "Retrieves coupon statistics for the supervisor")
//    public ResponseEntity<ApiResponse<CouponSummaryDto>> getCouponSummary(
//            @RequestHeader("X-User-Id") Long userId) {
//
//        log.info("GET /api/supervisors/payments/coupons - userId: {}", userId);
//
//        CouponSummaryDto summary = couponService.getCouponSummary(userId);
//
//        return ResponseEntity.ok(ApiResponse.success(summary));
//    }
    
//    /**
//     * Get payment history (placeholder - requires payment service integration)
//     */
//    @GetMapping("/history")
//    @Operation(summary = "Get payment history",
//               description = "Retrieves payment history for the supervisor")
//    public ResponseEntity<ApiResponse<String>> getPaymentHistory(
//            @RequestHeader("X-User-Id") Long userId,
//            @RequestParam(required = false) Long patientId) {
//
//        log.info("GET /api/supervisors/payments/history - userId: {}, patientId: {}", userId, patientId);
//
//        // TODO: Implement with payment service integration in Phase 5
//        return ResponseEntity.ok(ApiResponse.success(
//                "Payment history endpoint - to be implemented in Phase 5", null));
//    }
//
//    /**
//     * Pay consultation fee on behalf of patient
//     * Supports: STRIPE, PAYPAL, COUPON payment methods
//     */
//    @PostMapping("/pay")
//    @Operation(summary = "Pay consultation fee",
//            description = "Pay consultation fee on behalf of patient using Stripe, PayPal, or Coupon")
//    public ResponseEntity<ApiResponse<SupervisorPaymentResponseDto>> payConsultationFee(
//            @RequestHeader("X-User-Id") Long userId,
//            @Valid @RequestBody SupervisorPayConsultationDto dto) {
//
//        log.info("Processing consultation payment for case {} by supervisor userId: {}",
//                dto.getCaseId(), userId);
//
//        SupervisorPaymentResponseDto response = paymentManagementService.payConsultationFee(userId, dto);
//
//        return ResponseEntity.ok(ApiResponse.success("Payment processed successfully", response));
//    }

//    /**
//     * Create Stripe payment intent for consultation
//     */
//    @PostMapping("/create-payment-intent")
//    @Operation(summary = "Create payment intent",
//            description = "Create a Stripe payment intent for consultation fee")
//    public ResponseEntity<ApiResponse<PaymentIntentDto>> createPaymentIntent(
//            @RequestHeader("X-User-Id") Long userId,
//            @RequestParam @Parameter(description = "Case ID") Long caseId,
//            @RequestParam @Parameter(description = "Patient ID") Long patientId,
//            @RequestParam @Parameter(description = "Doctor ID") Long doctorId) {
//
//        log.info("Creating payment intent for case {} by supervisor userId: {}", caseId, userId);
//
//        PaymentIntentDto response = paymentManagementService
//                .createPaymentIntent(userId, caseId, patientId, doctorId);
//
//        return ResponseEntity.ok(ApiResponse.success(response));
//    }

//    /**
//     * Validate coupon before payment
//     */
//    @PostMapping("/validate-coupon")
//    @Operation(summary = "Validate coupon",
//            description = "Validate a coupon code before using it for payment")
//    public ResponseEntity<ApiResponse<CouponValidationResponseDto>> validateCoupon(
//            @RequestHeader("X-User-Id") Long userId,
//            @RequestParam @Parameter(description = "Coupon code") String couponCode,
//            @RequestParam @Parameter(description = "Patient ID") Long patientId,
//            @RequestParam @Parameter(description = "Case ID") Long caseId) {
//
//        log.info("Validating coupon {} for case {} by supervisor userId: {}",
//                couponCode, caseId, userId);
//
//        CouponValidationResponseDto response = paymentManagementService
//                .validateCouponForPayment(userId, couponCode, patientId, caseId);
//
//        return ResponseEntity.ok(ApiResponse.success(response));
//    }

//    /**
//     * Get available coupons for patient
//     */
//    @GetMapping("/coupons/patient/{patientId}")
//    @Operation(summary = "Get patient coupons",
//            description = "Get available coupons for a specific patient")
//    public ResponseEntity<ApiResponse<List<CouponDto>>> getPatientCoupons(
//            @RequestHeader("X-User-Id") Long userId,
//            @PathVariable @Parameter(description = "Patient ID") Long patientId) {
//
//        log.info("Getting coupons for patient {} by supervisor userId: {}", patientId, userId);
//
//        List<CouponDto> coupons = couponService.getAvailableCouponsForPatient(userId, patientId);
//
//        return ResponseEntity.ok(ApiResponse.success(coupons));
////    }
//
//    /**
//     * Get all available coupons for supervisor
//     */
//    @GetMapping("/available-coupons")
//    @Operation(summary = "Get all coupons",
//            description = "Get all available coupons for the supervisor")
//    public ResponseEntity<ApiResponse<List<CouponDto>>> getAllCoupons(
//            @RequestHeader("X-User-Id") Long userId) {
//
//        log.info("Getting all coupons for supervisor userId: {}", userId);
//
//        List<CouponDto> coupons = couponService.getAllAvailableCoupons(userId);
//
//        return ResponseEntity.ok(ApiResponse.success(coupons));
//    }
//
//    /**
//     * Get coupons expiring soon
//     */
//    @GetMapping("/coupons/expiring")
//    @Operation(summary = "Get expiring coupons",
//            description = "Get coupons expiring within specified days (default 7)")
//    public ResponseEntity<ApiResponse<List<CouponDto>>> getExpiringCoupons(
//            @RequestHeader("X-User-Id") Long userId,
//            @RequestParam(defaultValue = "7") @Parameter(description = "Days until expiry") int days) {
//
//        log.info("Getting coupons expiring in {} days for supervisor userId: {}", days, userId);
//
//        List<CouponDto> coupons = couponService.getCouponsExpiringSoon(userId, days);
//
//        return ResponseEntity.ok(ApiResponse.success(coupons));
//    }
//
//    /// /////////////////////////////

    // ==================== Payment Processing ====================

    /**
     * Pay consultation fee
     * Supports multiple payment methods: STRIPE, PAYPAL, COUPON
     */
    @PostMapping("/pay")
    @Operation(summary = "Pay consultation fee",
            description = "Process payment for a case consultation using Stripe, PayPal, or Coupon")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> payConsultationFee(
            @Valid @RequestBody PayConsultationFeeRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("User {} processing payment for case {} via {}",
                userId, request.getCaseId(), request.getPaymentMethod());

        PaymentResponseDto response = paymentService.payConsultationFee(userId, request);

        return ResponseEntity.ok(ApiResponse.success("Payment processed successfully", response));
    }

    /**
     * Create Stripe payment intent
     */
    @PostMapping("/create-payment-intent")
    @Operation(summary = "Create Stripe payment intent",
            description = "Create a payment intent for Stripe checkout")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> createPaymentIntent(
            @RequestParam Long caseId,
            @RequestParam Long patientId,
            @RequestParam Long doctorId,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("User {} creating payment intent for case {}", userId, caseId);

        PaymentResponseDto response = paymentService.createStripePaymentIntent(
                userId, caseId, patientId, doctorId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== Coupon Validation ====================

    /**
     * Validate coupon for payment
     */
    @PostMapping("/validate-coupon")
    @Operation(summary = "Validate coupon for payment",
            description = "Validate a coupon code before using it for case payment")
    public ResponseEntity<ApiResponse<CouponValidationResponse>> validateCoupon(
            @RequestParam String couponCode,
            @RequestParam Long patientId,
            @RequestParam Long caseId,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("User {} validating coupon {} for patient {} case {}",
                userId, couponCode, patientId, caseId);

        CouponValidationResponse response = couponService.validateCoupon(
                userId, couponCode, patientId, caseId, null);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== Legacy Coupon Redemption ====================

    /**
     * Redeem coupon for case payment (Legacy endpoint)
     * Note: Prefer using /pay with paymentMethod=COUPON instead
     */
    @PostMapping("/coupon/{caseId}")
    @Operation(summary = "Redeem coupon (Legacy)",
            description = "Legacy endpoint for coupon redemption. Use /pay with paymentMethod=COUPON instead.")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> redeemCoupon(
            @PathVariable Long caseId,
            @RequestParam Long patientId,
            @RequestBody CouponRedemptionRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("User {} redeeming coupon {} for case {} (legacy endpoint)",
                userId, request.getCouponCode(), caseId);

        // Convert to new payment request
        PayConsultationFeeRequest paymentRequest = PayConsultationFeeRequest.builder()
                .caseId(caseId)
                .patientId(patientId)
                .paymentMethod("COUPON")
                .couponCode(request.getCouponCode())
                .build();

        PaymentResponseDto response = paymentService.payConsultationFee(userId, paymentRequest);

        return ResponseEntity.ok(ApiResponse.success("Coupon redeemed successfully", response));
    }

    // ==================== Coupon Summary (Legacy Endpoints) ====================

    /**
     * Get coupon summary
     */
    @GetMapping("/coupons")
    @Operation(summary = "Get coupon summary",
            description = "Get coupon statistics for the supervisor")
    public ResponseEntity<ApiResponse<com.commonlibrary.dto.coupon.CouponSummaryDto>> getCouponSummary(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("User {} fetching coupon summary", userId);

        com.commonlibrary.dto.coupon.CouponSummaryDto summary = couponService.getCouponSummary(userId);

        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * Get available coupons for patient
     */
    @GetMapping("/coupons/patient/{patientId}")
    @Operation(summary = "Get patient coupons",
            description = "Get available coupons for a specific patient")
    public ResponseEntity<ApiResponse<List<SupervisorCouponAllocationDto>>> getPatientCoupons(
            @PathVariable Long patientId,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("User {} fetching coupons for patient {}", userId, patientId);

        List<SupervisorCouponAllocationDto> coupons = couponService.getAvailableCouponsForPatient(
                userId, patientId);

        return ResponseEntity.ok(ApiResponse.success(coupons));
    }

    /**
     * Get all available coupons
     */
    @GetMapping("/available-coupons")
    @Operation(summary = "Get all available coupons",
            description = "Get all coupons that are assigned and ready for use")
    public ResponseEntity<ApiResponse<List<SupervisorCouponAllocationDto>>> getAllAvailableCoupons(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("User {} fetching all available coupons", userId);

        List<SupervisorCouponAllocationDto> coupons = couponService.getAllCouponsForSupervisor(userId);

        return ResponseEntity.ok(ApiResponse.success(coupons));
    }

    /**
     * Get expiring coupons
     */
    @GetMapping("/coupons/expiring")
    @Operation(summary = "Get expiring coupons",
            description = "Get coupons expiring within specified days")
    public ResponseEntity<ApiResponse<List<SupervisorCouponAllocationDto>>> getExpiringCoupons(
            @RequestParam(defaultValue = "7")
            @Parameter(description = "Days until expiration") int days,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("User {} fetching coupons expiring in {} days", userId, days);

        List<SupervisorCouponAllocationDto> coupons = couponService.getExpiringCoupons(userId, days);

        return ResponseEntity.ok(ApiResponse.success(coupons));
    }

    // ==================== Payment History ====================

    /**
     * Get payment history for supervisor
     * Returns payment history for all assigned patients or specific patient
     * Includes payments made via COUPON, STRIPE, and PAYPAL methods
     */
    @GetMapping("/history")
    @Operation(summary = "Get payment history",
            description = "Get payment history for supervisor. Returns payments for all assigned patients " +
                    "or filter by specific patient ID. Includes all payment methods: COUPON, STRIPE, PAYPAL")
    public ResponseEntity<ApiResponse<List<PaymentHistoryDto>>> getPaymentHistory(
            @RequestParam(required = false)
            @Parameter(description = "Optional patient ID to filter history. If not provided, returns for all assigned patients")
            Long patientId,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("User {} fetching payment history (patientId filter: {})", userId, patientId);

        List<PaymentHistoryDto> history = paymentService.getPaymentHistory(userId, patientId);

        String message = patientId != null
                ? String.format("Payment history retrieved for patient %d", patientId)
                : String.format("Payment history retrieved for all assigned patients (%d records)", history.size());

        return ResponseEntity.ok(ApiResponse.success(message, history));
    }

}
