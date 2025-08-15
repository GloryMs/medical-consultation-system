package com.adminservice.controller;

import com.adminservice.dto.*;
import com.adminservice.entity.Complaint;
import com.adminservice.entity.SystemConfig;
import com.adminservice.service.AdminService;
import com.commonlibrary.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardDto>> getDashboard() {
        DashboardDto dashboard = adminService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    @PostMapping("/doctors/verify")
    public ResponseEntity<ApiResponse<Void>> verifyDoctor(
            @Valid @RequestBody VerifyDoctorDto dto) {
        adminService.verifyDoctor(dto);
        return ResponseEntity.ok(ApiResponse.success(null, "Doctor verification updated"));
    }

    // 26. Get System Metrics - MISSING ENDPOINT
    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<SystemMetricsDto>> getSystemMetrics() {
        SystemMetricsDto metrics = adminService.getSystemMetrics();
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    // 27. Generate Revenue Report - MISSING ENDPOINT
    @GetMapping("/reports/revenue")
    public ResponseEntity<ApiResponse<RevenueReportDto>> generateRevenueReport(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        RevenueReportDto report = adminService.generateRevenueReport(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    // 25 & 28. Get Pending Verifications - MISSING ENDPOINT
    @GetMapping("/doctors/pending-verification")
    public ResponseEntity<ApiResponse<List<PendingVerificationDto>>> getPendingVerifications() {
        List<PendingVerificationDto> pending = adminService.getPendingVerifications();
        return ResponseEntity.ok(ApiResponse.success(pending));
    }

    // 29. Get All Users - MISSING ENDPOINT
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserDto>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        Page<UserDto> users = adminService.getAllUsers(page, size, role, status);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    // 30. Generate Doctor Performance Report - MISSING ENDPOINT
    @GetMapping("/reports/doctor-performance")
    public ResponseEntity<ApiResponse<DoctorPerformanceReportDto>> generateDoctorPerformanceReport(
            @RequestParam Long doctorId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        DoctorPerformanceReportDto report = adminService.generateDoctorPerformanceReport(doctorId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    // 31. Enable/Disable User Account - MISSING ENDPOINTS
    @PutMapping("/users/{userId}/disable")
    public ResponseEntity<ApiResponse<Void>> disableUserAccount(
            @PathVariable Long userId,
            @Valid @RequestBody DisableAccountDto dto) {
        adminService.disableUserAccount(userId, dto.getReason());
        return ResponseEntity.ok(ApiResponse.success(null, "Account disabled"));
    }

    @PutMapping("/users/{userId}/enable")
    public ResponseEntity<ApiResponse<Void>> enableUserAccount(@PathVariable Long userId) {
        adminService.enableUserAccount(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Account enabled"));
    }

    // 32. Reset Password - MISSING ENDPOINT
    @PostMapping("/users/{userId}/reset-password")
    public ResponseEntity<ApiResponse<ResetPasswordResponseDto>> resetUserPassword(@PathVariable Long userId) {
        ResetPasswordResponseDto response = adminService.resetUserPassword(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Password reset successfully"));
    }

    // 33. Update System Configuration - MISSING ENDPOINT
    @PutMapping("/config")
    public ResponseEntity<ApiResponse<SystemConfig>> updateSystemConfig(
            @Valid @RequestBody SystemConfigDto dto) {
        SystemConfig config = adminService.updateSystemConfig(dto);
        return ResponseEntity.ok(ApiResponse.success(config, "Configuration updated"));
    }

    // 34. Manage Specializations - MISSING ENDPOINT
    @PostMapping("/config/specializations")
    public ResponseEntity<ApiResponse<Void>> manageSpecializations(
            @Valid @RequestBody SpecializationsDto dto) {
        adminService.manageSpecializations(dto);
        return ResponseEntity.ok(ApiResponse.success(null, "Specializations updated"));
    }

    // 35. Update Static Content - MISSING ENDPOINT
    @PutMapping("/content/static")
    public ResponseEntity<ApiResponse<Void>> updateStaticContent(
            @Valid @RequestBody StaticContentDto dto) {
        adminService.updateStaticContent(dto);
        return ResponseEntity.ok(ApiResponse.success(null, "Content updated"));
    }

    // 36. Get All Complaints - MISSING ENDPOINT
    @GetMapping("/complaints")
    public ResponseEntity<ApiResponse<List<Complaint>>> getAllComplaints(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        List<Complaint> complaints = adminService.getAllComplaints(status, priority);
        return ResponseEntity.ok(ApiResponse.success(complaints));
    }

    // 37. System Usage Analytics - MISSING ENDPOINT
    @GetMapping("/analytics/usage")
    public ResponseEntity<ApiResponse<SystemUsageAnalyticsDto>> getSystemUsageAnalytics(
            @RequestParam(defaultValue = "MONTHLY") String period) {
        SystemUsageAnalyticsDto analytics = adminService.getSystemUsageAnalytics(period);
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    // 38. Respond to Complaint - MISSING ENDPOINT
    @PostMapping("/complaints/{complaintId}/respond")
    public ResponseEntity<ApiResponse<Void>> respondToComplaint(
            @PathVariable Long complaintId,
            @Valid @RequestBody ComplaintResponseDto dto) {
        adminService.respondToComplaint(complaintId, dto);
        return ResponseEntity.ok(ApiResponse.success(null, "Response sent"));
    }

    // View All Payment Records - Already exists in original implementation
    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<List<PaymentRecordDto>>> getAllPaymentRecords(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        List<PaymentRecordDto> records = adminService.getAllPaymentRecords(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(records));
    }

    @GetMapping("/payments/subscriptions")
    public ResponseEntity<ApiResponse<List<SubscriptionPaymentDto>>> getSubscriptionPayments() {
        List<SubscriptionPaymentDto> payments = adminService.getSubscriptionPayments();
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @GetMapping("/payments/consultations")
    public ResponseEntity<ApiResponse<List<ConsultationPaymentDto>>> getConsultationPayments() {
        List<ConsultationPaymentDto> payments = adminService.getConsultationPayments();
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @PostMapping("/payments/refund")
    public ResponseEntity<ApiResponse<Void>> processRefund(
            @Valid @RequestBody RefundRequestDto dto) {
        adminService.processRefund(dto);
        return ResponseEntity.ok(ApiResponse.success(null, "Refund processed"));
    }
}
