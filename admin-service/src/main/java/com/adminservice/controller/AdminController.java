package com.adminservice.controller;

import com.adminservice.dto.*;
import com.adminservice.entity.Complaint;
import com.adminservice.entity.SystemConfig;
import com.adminservice.feign.AuthServiceClient;
import com.adminservice.feign.CommonConfigClient;
import com.adminservice.service.AdminCaseService;
import com.adminservice.service.AdminService;
import com.adminservice.service.ComplaintService;
import com.commonlibrary.dto.*;
import com.commonlibrary.dto.PendingVerificationDto;
import com.commonlibrary.dto.UserDto;
import com.commonlibrary.entity.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final CommonConfigClient configService;
    private final AuthServiceClient authServiceClient;
    private final ComplaintService complaintService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardDto>> getDashboard() {
        DashboardDto dashboard = adminService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<UserStasDto>> getStats() {
        UserStasDto stats = adminService.getUserStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @PostMapping("/doctors/verify")
    public ResponseEntity<ApiResponse<Void>> verifyDoctor(
            @Valid @RequestBody VerifyDoctorDto dto) {
        adminService.verifyDoctor(dto);
        return ResponseEntity.ok(ApiResponse.success(null, "Doctor verification updated"));
    }

    /**
     * Verify Doctor - Approve or Reject
     * POST /api/admin/doctors/{doctorId}/verify
     * Request body example:
     * {
     *   "doctorId": 1,
     *   "approved": true,
     *   "reason": "Credentials verified",  // optional if approved
     *   "notes": "All documents are valid"  // optional
     * }
     */
    @PostMapping("/doctors/{doctorId}/verify")
    public ResponseEntity<ApiResponse<DoctorVerificationResponseDto>> verifyDoctor(
            @PathVariable Long doctorId,
            @Valid @RequestBody VerifyDoctorRequestDto verificationData) {

        // Ensure doctorId in path matches body (if present)
        if (verificationData.getDoctorId() != null &&
                !verificationData.getDoctorId().equals(doctorId)) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Doctor ID in path and body must match", HttpStatus.BAD_REQUEST)
            );
        }

        verificationData.setDoctorId(doctorId);

        DoctorVerificationResponseDto response = adminService.verifyDoctor(doctorId, verificationData);

        String message = verificationData.getApproved()
                ? "Doctor verified and approved successfully"
                : "Doctor verification rejected";

        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    /**
     * Reject Doctor Verification
     * POST /api/admin/doctors/{doctorId}/reject
     * Request body example:
     * {
     *   "reason": "Invalid medical license"
     * }
     */
    @PostMapping("/doctors/{doctorId}/reject")
    public ResponseEntity<ApiResponse<DoctorVerificationResponseDto>> rejectDoctorVerification(
            @PathVariable Long doctorId,
            @Valid @RequestBody RejectDoctorRequestDto request) {

        DoctorVerificationResponseDto response =
                adminService.rejectDoctorVerification(doctorId, request.getReason());

        return ResponseEntity.ok(ApiResponse.success(response, "Doctor verification rejected"));
    }

    /**
     * Update Doctor Status
     * PUT /api/admin/doctors/{doctorId}/status
     * Request body example:
     * {
     *   "status": "INACTIVE",
     *   "reason": "Doctor requested leave"  // optional
     * }
     */
    @PutMapping("/doctors/{doctorId}/status")
    public ResponseEntity<ApiResponse<Void>> updateDoctorStatus(
            @PathVariable Long doctorId,
            @Valid @RequestBody UpdateDoctorStatusRequestDto request) {

        adminService.updateDoctorStatus(doctorId, request.getStatus());

        return ResponseEntity.ok(ApiResponse.success(
                null,
                "Doctor status updated to " + request.getStatus() + " successfully"
        ));
    }

    /**
     * Get All Doctors with Filters
     * GET /api/admin/doctors
     * Query parameters (all optional):
     * - searchTerm: Search in name, license, email
     * - verificationStatus: PENDING, VERIFIED, REJECTED
     * - specialization: Primary specialization
     * - isAvailable: true/false
     * - emergencyMode: true/false
     * - minYearsExperience: Minimum years of experience
     * - maxYearsExperience: Maximum years of experience
     * - minRating: Minimum rating (0-5)
     * - city: City filter
     * - country: Country filter
     * - page: Page number (default 0)
     * - size: Page size (default 20)
     * - sortBy: Sort field (default createdAt)
     * - sortDirection: ASC or DESC (default DESC)
     *
     * Example: GET /api/admin/doctors?verificationStatus=PENDING&specialization=Cardiology&page=0&size=10
     */
    @GetMapping("/doctors")
    public ResponseEntity<ApiResponse<Page<DoctorSummaryDto>>> getAllDoctors(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) VerificationStatus verificationStatus,
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) Boolean isAvailable,
            @RequestParam(required = false) Boolean emergencyMode,
            @RequestParam(required = false) Integer minYearsExperience,
            @RequestParam(required = false) Integer maxYearsExperience,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        // Build filter DTO
        DoctorFilterDto filters = DoctorFilterDto.builder()
                .searchTerm(searchTerm)
                .verificationStatus(verificationStatus)
                .specialization(specialization)
                .isAvailable(isAvailable)
                .emergencyMode(emergencyMode)
                .minYearsExperience(minYearsExperience)
                .maxYearsExperience(maxYearsExperience)
                .minRating(minRating)
                .city(city)
                .country(country)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        Page<DoctorSummaryDto> doctors = adminService.getAllDoctors(filters);

        return ResponseEntity.ok(ApiResponse.success(
                doctors,
                String.format("Retrieved %d doctors", doctors.getNumberOfElements())
        ));
    }


    @GetMapping ("/notifications/{userId}")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getNotification(@PathVariable Long userId){
        List<NotificationDto> notificationsDto = new ArrayList<>();
        notificationsDto = adminService.getMyNotificationsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(notificationsDto));
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

    @GetMapping("/doctors/{doctorId}/verification")
    public ResponseEntity<ApiResponse<DoctorVerificationDetailsDto>> getDoctorVerificationDetails(
            @PathVariable Long doctorId) {

        DoctorVerificationDetailsDto details = adminService.getDoctorVerificationDetails(doctorId);
        return ResponseEntity.ok(ApiResponse.success(details, "Doctor verification details retrieved successfully"));
    }

    // 29. Get All Users - MISSING ENDPOINT
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserDto>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status) {
        Page<UserDto> users = authServiceClient.getAllUsers(page, size, role, status).getBody().getData();
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
    public ResponseEntity<ApiResponse<List<ComplaintDto>>> getAllComplaints(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        List<ComplaintDto> complaints = complaintService.getAllComplaints(status, priority);
        return ResponseEntity.ok(ApiResponse.success(complaints));
    }

    @PostMapping("/complaints")
    public ResponseEntity<ApiResponse<Void>> submitComplaint(@RequestBody ComplaintDto complaintDto) {
        Complaint complaint = complaintService.submitComplaint(complaintDto);
        if( complaint != null ){
            return ResponseEntity.ok(ApiResponse.success(null, "Complaint submitted"));
        }
            return ResponseEntity.ok(ApiResponse.error("Something went wrong while submitting the complaint ..", HttpStatus.BAD_REQUEST));
    }

    @GetMapping("/complaints/{patientId}")
    public ResponseEntity<ApiResponse<List<ComplaintDto>>> getPatientComplaintsById(
            @PathVariable Long patientId) {
        List<ComplaintDto> complaints = complaintService.getPatientComplaints(patientId);
        return ResponseEntity.ok(ApiResponse.success(complaints));
    }

    // 38. Respond to Complaint - MISSING ENDPOINT
    @PostMapping("/complaints/{complaintId}/respond")
    public ResponseEntity<ApiResponse<Void>> respondToComplaint(
            @PathVariable Long complaintId,
            @Valid @RequestBody ComplaintResponseDto dto) {
        complaintService.respondToComplaint(complaintId, dto);
        return ResponseEntity.ok(ApiResponse.success(null, "Response sent"));
    }

    // 37. System Usage Analytics - MISSING ENDPOINT
    @GetMapping("/analytics/usage")
    public ResponseEntity<ApiResponse<SystemUsageAnalyticsDto>> getSystemUsageAnalytics(
            @RequestParam(defaultValue = "MONTHLY") String period) {
        SystemUsageAnalyticsDto analytics = adminService.getSystemUsageAnalytics(period);
        return ResponseEntity.ok(ApiResponse.success(analytics));
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

    @GetMapping("/config/diseases")
    public ResponseEntity<ApiResponse<List<DiseaseDto>>> getAllDiseases() {
        List<DiseaseDto> diseases = configService.getAllDiseases();
        return ResponseEntity.ok(ApiResponse.success(diseases));
    }

    @GetMapping("/config/diseases/category/{category}")
    public ResponseEntity<ApiResponse<List<DiseaseDto>>> getDiseasesByCategory(@PathVariable String category) {
        List<DiseaseDto> diseases = configService.getAllActiveDiseasesByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(diseases));
    }

    @GetMapping("/config/medications")
    public ResponseEntity<ApiResponse<List<MedicationDto>>> getAllMedications() {
        List<MedicationDto> medications = configService.getAllMedications();
        return ResponseEntity.ok(ApiResponse.success(medications));
    }

    @GetMapping("/config/symptoms")
    public ResponseEntity<ApiResponse<List<SymptomDto>>> getAllSymptoms() {
        List<SymptomDto> symptoms = configService.getAllActiveSymptoms();
        return ResponseEntity.ok(ApiResponse.success(symptoms));
    }

    @GetMapping("/config/symptoms/system/{bodySystem}")
    public ResponseEntity<ApiResponse<List<SymptomDto>>> getSymptomsByBodySystem(@PathVariable String bodySystem) {
        List<SymptomDto> symptoms = configService.getSymptomsByBodySystem(bodySystem);
        return ResponseEntity.ok(ApiResponse.success(symptoms));
    }

    @GetMapping("/config/{configType}")
    public ResponseEntity<ApiResponse<List<MedicalConfigurationDto>>> getConfigurationsByType
            (@PathVariable String configType) {
        List<MedicalConfigurationDto> configs = configService.getConfigurationsByType(configType);
        return ResponseEntity.ok(ApiResponse.success(configs));
    }

    /*TODO
    *  Update clear cache functionality*/

    @PostMapping("/config/cache/clear")
    public ResponseEntity<ApiResponse<Void>> clearCache() {
        //configService.clearCache();
        return ResponseEntity.ok(ApiResponse.success(null, "Cache cleared successfully"));
    }

    /**
     * Suspend a user account
     * POST /api/admin/users/{userId}/suspend
     */
    @PostMapping("/users/{userId}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspendUser(
            @PathVariable Long userId,
            @RequestBody SuspendUserDto dto) {
        adminService.suspendUser(userId, dto.getReason());
        return ResponseEntity.ok(ApiResponse.success(null, "User suspended successfully"));
    }

    /**
     * Unsuspend (Activate) a user account
     * POST /api/admin/users/{userId}/unsuspend
     */
    @PostMapping("/users/{userId}/unsuspend")
    public ResponseEntity<ApiResponse<Void>> unsuspendUser(@PathVariable Long userId) {
        adminService.unsuspendUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User activated successfully"));
    }

    /**
     * Delete a user account permanently
     * DELETE /api/admin/users/{userId}
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
    }

    /**
     * Get a specific user by ID
     * GET /api/admin/users/{userId}
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long userId) {
        UserDto user = adminService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}
