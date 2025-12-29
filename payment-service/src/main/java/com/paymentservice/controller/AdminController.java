package com.paymentservice.controller;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.PaymentAnalyticsDto;
import com.paymentservice.dto.DoctorBalanceDto;
import com.paymentservice.dto.PayoutRequestDto;
import com.paymentservice.dto.PayoutResponseDto;
import com.paymentservice.entity.ConsultationFee;
import com.paymentservice.service.ConsultationFeeService;
import com.paymentservice.service.DoctorBalanceService;
import com.paymentservice.service.PaymentAnalyticsService;
import com.paymentservice.service.RefundService;
import com.paymentservice.service.SystemConfigurationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for admin operations
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    
    private final ConsultationFeeService consultationFeeService;
    private final DoctorBalanceService doctorBalanceService;
    private final RefundService refundService;
    private final SystemConfigurationService systemConfigService;
    private final PaymentAnalyticsService paymentAnalyticsService;
    
    // ===== Consultation Fee Management =====
    
    /**
     * Get all consultation fees
     */
    @GetMapping("/consultation-fees")
    public ResponseEntity<ApiResponse<List<ConsultationFee>>> getAllConsultationFees() {
        List<ConsultationFee> fees = consultationFeeService.getAllActiveFees();
        return ResponseEntity.ok(ApiResponse.success(fees));
    }
    
    /**
     * Get consultation fee for specialization
     */
    @GetMapping("/consultation-fees/{specialization}")
    public ResponseEntity<ApiResponse<BigDecimal>> getConsultationFee(
            @PathVariable String specialization) {
        BigDecimal fee = consultationFeeService.getFeeBySpecialization(specialization);
        return ResponseEntity.ok(ApiResponse.success(fee));
    }
    
    /**
     * Set consultation fee for specialization
     */
    @PostMapping("/consultation-fees")
    public ResponseEntity<ApiResponse<ConsultationFee>> setConsultationFee(
            @RequestParam String specialization,
            @RequestParam BigDecimal feeAmount,
            @RequestParam Long adminId,
            @RequestParam String reason) {
        
        ConsultationFee fee = consultationFeeService
            .setFeeForSpecialization(specialization, feeAmount, adminId, reason);
        
        return ResponseEntity.ok(ApiResponse.success(fee, 
            "Consultation fee updated successfully"));
    }
    
    /**
     * Bulk update consultation fees
     */
    @PostMapping("/consultation-fees/bulk")
    public ResponseEntity<ApiResponse<Void>> bulkUpdateFees(
            @RequestBody Map<String, BigDecimal> specializationFees,
            @RequestParam Long adminId,
            @RequestParam String reason) {
        
        consultationFeeService.bulkUpdateFees(specializationFees, adminId, reason);
        
        return ResponseEntity.ok(ApiResponse.success(null, 
            "Consultation fees updated successfully"));
    }
    
    /**
     * Enable unified fee
     */
    @PostMapping("/consultation-fees/unified/enable")
    public ResponseEntity<ApiResponse<Void>> enableUnifiedFee(
            @RequestParam BigDecimal unifiedFeeAmount,
            @RequestParam Long adminId) {
        
        consultationFeeService.enableUnifiedFee(unifiedFeeAmount, adminId);
        
        return ResponseEntity.ok(ApiResponse.success(null, 
            "Unified fee enabled successfully"));
    }
    
    /**
     * Disable unified fee
     */
    @PostMapping("/consultation-fees/unified/disable")
    public ResponseEntity<ApiResponse<Void>> disableUnifiedFee(
            @RequestParam Long adminId) {
        
        consultationFeeService.disableUnifiedFee(adminId);
        
        return ResponseEntity.ok(ApiResponse.success(null, 
            "Unified fee disabled successfully"));
    }
    
    /**
     * Get fee statistics
     */
    @GetMapping("/consultation-fees/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFeeStatistics() {
        Map<String, Object> stats = consultationFeeService.getFeeStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    // ===== Doctor Balance Management =====
    
    /**
     * Get all doctor balances
     */
    @GetMapping("/doctor-balances")
    public ResponseEntity<ApiResponse<List<DoctorBalanceDto>>> getAllDoctorBalances() {
        List<DoctorBalanceDto> balances = doctorBalanceService.getAllDoctorBalances();
        return ResponseEntity.ok(ApiResponse.success(balances));
    }
    
    /**
     * Get doctor balance
     */
    @GetMapping("/doctor-balances/{doctorId}")
    public ResponseEntity<ApiResponse<DoctorBalanceDto>> getDoctorBalance(
            @PathVariable Long doctorId) {
        DoctorBalanceDto balance = doctorBalanceService.getDoctorBalance(doctorId);
        return ResponseEntity.ok(ApiResponse.success(balance));
    }
    
    /**
     * Process doctor payout
     */
    @PostMapping("/doctor-payouts")
    public ResponseEntity<ApiResponse<PayoutResponseDto>> processPayout(
            @Valid @RequestBody PayoutRequestDto payoutRequest) {
        try {
            PayoutResponseDto response = doctorBalanceService.processPayout(payoutRequest);
            return ResponseEntity.ok(ApiResponse.success(response, 
                "Payout processed successfully"));
        } catch (Exception e) {
            log.error("Failed to process payout", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to process payout: " + e.getMessage(), HttpStatus.BAD_REQUEST));
        }
    }
    
    /**
     * Process automatic payouts
     */
    @PostMapping("/doctor-payouts/automatic")
    public ResponseEntity<ApiResponse<Void>> processAutomaticPayouts() {
        doctorBalanceService.processAutomaticPayouts();
        return ResponseEntity.ok(ApiResponse.success(null, 
            "Automatic payouts initiated"));
    }
    
    /**
     * Enable doctor payout
     */
    @PostMapping("/doctor-payouts/{doctorId}/enable")
    public ResponseEntity<ApiResponse<Void>> enablePayout(
            @PathVariable Long doctorId,
            @RequestParam String stripeConnectAccountId) {
        
        doctorBalanceService.enablePayout(doctorId, stripeConnectAccountId);
        
        return ResponseEntity.ok(ApiResponse.success(null, 
            "Payout enabled for doctor"));
    }
    
    /**
     * Disable doctor payout
     */
    @PostMapping("/doctor-payouts/{doctorId}/disable")
    public ResponseEntity<ApiResponse<Void>> disablePayout(
            @PathVariable Long doctorId,
            @RequestParam String reason) {
        
        doctorBalanceService.disablePayout(doctorId, reason);
        
        return ResponseEntity.ok(ApiResponse.success(null, 
            "Payout disabled for doctor"));
    }
    
    /**
     * Get platform balance statistics
     */
    @GetMapping("/platform/balance-statistics")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> getPlatformBalanceStatistics() {
        Map<String, BigDecimal> stats = doctorBalanceService.getPlatformBalanceStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    // ===== Refund Management =====
    
    /**
     * Get refund statistics
     */
    @GetMapping("/refunds/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRefundStatistics(
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {

        Map<String, Object> stats = refundService.getRefundStatistics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // ===== Payment Analytics =====

    /**
     * Get comprehensive payment analytics
     *
     * @param startDate Start of analytics period (optional, defaults to 30 days ago)
     * @param endDate End of analytics period (optional, defaults to now)
     * @return Payment analytics data including overview, revenue, transactions, payment methods, trends, and refunds
     */
    @GetMapping("/payments/analytics")
    public ResponseEntity<ApiResponse<PaymentAnalyticsDto>> getPaymentAnalytics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        try {
            // Parse ISO 8601 datetime strings with timezone to LocalDateTime
            LocalDateTime start = startDate != null ? parseDateTime(startDate) : null;
            LocalDateTime end = endDate != null ? parseDateTime(endDate) : null;

            PaymentAnalyticsDto analytics = paymentAnalyticsService.getPaymentAnalytics(start, end);

            String message = String.format("Payment analytics generated for %d payments",
                    analytics.getTotalPaymentsAnalyzed());

            return ResponseEntity.ok(ApiResponse.success(analytics, message));
        } catch (IllegalArgumentException e) {
            log.error("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), HttpStatus.BAD_REQUEST));
        } catch (Exception e) {
            log.error("Failed to generate payment analytics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to generate analytics: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * Parse ISO 8601 datetime string (with or without timezone) to LocalDateTime
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }

        try {
            // Handle ISO 8601 format with 'Z' timezone indicator
            if (dateTimeStr.endsWith("Z")) {
                return java.time.Instant.parse(dateTimeStr)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime();
            }
            // Handle ISO 8601 format with timezone offset
            else if (dateTimeStr.contains("+") || dateTimeStr.lastIndexOf('-') > 10) {
                return java.time.OffsetDateTime.parse(dateTimeStr).toLocalDateTime();
            }
            // Handle plain LocalDateTime format
            else {
                return LocalDateTime.parse(dateTimeStr);
            }
        } catch (Exception e) {
            log.error("Failed to parse datetime: {}", dateTimeStr, e);
            throw new IllegalArgumentException("Invalid datetime format: " + dateTimeStr +
                    ". Expected ISO 8601 format (e.g., 2025-11-28T06:22:24.224Z)");
        }
    }

    // ===== System Configuration =====
    
    /**
     * Get doctor trial period
     */
    @GetMapping("/configuration/doctor-trial-period")
    public ResponseEntity<ApiResponse<Integer>> getDoctorTrialPeriod() {
        Integer trialPeriod = systemConfigService.getDoctorTrialPeriod();
        return ResponseEntity.ok(ApiResponse.success(trialPeriod));
    }
    
    /**
     * Set doctor trial period
     */
    @PutMapping("/configuration/doctor-trial-period")
    public ResponseEntity<ApiResponse<Void>> setDoctorTrialPeriod(
            @RequestParam Integer days,
            @RequestParam Long adminId) {
        
        systemConfigService.setDoctorTrialPeriod(days, adminId);
        
        return ResponseEntity.ok(ApiResponse.success(null, 
            "Doctor trial period updated successfully"));
    }
    
    /**
     * Get platform fee percentage
     */
    @GetMapping("/configuration/platform-fee")
    public ResponseEntity<ApiResponse<BigDecimal>> getPlatformFeePercentage() {
        BigDecimal fee = systemConfigService.getPlatformFeePercentage();
        return ResponseEntity.ok(ApiResponse.success(fee));
    }
    
    /**
     * Set platform fee percentage
     */
    @PutMapping("/configuration/platform-fee")
    public ResponseEntity<ApiResponse<Void>> setPlatformFeePercentage(
            @RequestParam BigDecimal percentage,
            @RequestParam Long adminId) {
        
        systemConfigService.setPlatformFeePercentage(percentage, adminId);
        
        return ResponseEntity.ok(ApiResponse.success(null, 
            "Platform fee percentage updated successfully"));
    }
}