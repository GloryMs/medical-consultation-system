package com.patientservice.controller;

import com.commonlibrary.dto.ApiResponse;
import com.patientservice.dto.*;
import com.patientservice.entity.Case;
import com.patientservice.entity.Complaint;
import com.patientservice.service.ComplaintService;
import com.patientservice.service.PatientService;
import com.patientservice.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final ComplaintService complaintService;
    private final ReportService reportService;

    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<PatientProfileDto>> createProfile(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody PatientProfileDto dto) {
        PatientProfileDto profile = patientService.createProfile(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(profile, "Profile created successfully"));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<PatientProfileDto>> getProfile(
            @RequestHeader("X-User-Id") Long userId) {
        PatientProfileDto profile = patientService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PostMapping("/subscription")
    public ResponseEntity<ApiResponse<SubscriptionDto>> createSubscription(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody SubscriptionDto dto) {
        SubscriptionDto subscription = patientService.createSubscription(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(subscription, "Subscription created successfully"));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<PatientProfileDto>> updateProfile(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody PatientProfileDto dto) {
        PatientProfileDto profile = patientService.updateProfile(userId, dto);
        return ResponseEntity.ok(ApiResponse.success(profile, "Profile updated successfully"));
    }

    @GetMapping("/subscription")
    public ResponseEntity<ApiResponse<SubscriptionStatusDto>> getSubscriptionStatus(
            @RequestHeader("X-User-Id") Long userId) {
        SubscriptionStatusDto status = patientService.getSubscriptionStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @GetMapping("/cases/{caseId}")
    public ResponseEntity<ApiResponse<CaseDetailsDto>> getCaseDetails(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long caseId) {
        CaseDetailsDto caseDetails = patientService.getCaseDetails(userId, caseId);
        return ResponseEntity.ok(ApiResponse.success(caseDetails));
    }

    @PostMapping("/cases/{caseId}/reschedule-request")
    public ResponseEntity<ApiResponse<Void>> requestReschedule(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long caseId,
            @Valid @RequestBody RescheduleRequestDto dto) {
        patientService.requestReschedule(userId, caseId, dto);
        return ResponseEntity.ok(ApiResponse.success(null, "Reschedule request sent"));
    }

    @GetMapping("/payments/history")
    public ResponseEntity<ApiResponse<List<PaymentHistoryDto>>> getPaymentHistory(
            @RequestHeader("X-User-Id") Long userId) {
        List<PaymentHistoryDto> history = patientService.getPaymentHistory(userId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    // 6. Update Case Status - MISSING ENDPOINT (Internal use by other services)
    @PutMapping("/cases/{caseId}/status")
    public ResponseEntity<ApiResponse<Void>> updateCaseStatus(
            @PathVariable Long caseId,
            @RequestParam String status,
            @RequestParam Long doctorId) {
        patientService.updateCaseStatus(caseId, status, doctorId);
        return ResponseEntity.ok(ApiResponse.success(null, "Case status updated"));
    }

    // 7. Submit Complaint - MISSING ENDPOINT
    @PostMapping("/complaints")
    public ResponseEntity<ApiResponse<Complaint>> submitComplaint(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ComplaintDto dto) {
        Complaint complaint = complaintService.submitComplaint(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(complaint, "Complaint submitted successfully"));
    }

    // 8. View My Complaints - MISSING ENDPOINT
    @GetMapping("/complaints")
    public ResponseEntity<ApiResponse<List<Complaint>>> getMyComplaints(
            @RequestHeader("X-User-Id") Long userId) {
        List<Complaint> complaints = complaintService.getPatientComplaints(userId);
        return ResponseEntity.ok(ApiResponse.success(complaints));
    }

    // 9. Generate Patient Report - MISSING ENDPOINT
    @GetMapping("/reports/medical-history")
    public ResponseEntity<ApiResponse<PatientReportDto>> generateMedicalHistoryReport(
            @RequestHeader("X-User-Id") Long userId) {
        PatientReportDto report = reportService.generateMedicalHistoryReport(userId);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @PostMapping("/cases")
    public ResponseEntity<ApiResponse<Case>> createCase(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateCaseDto dto) {
        Case medicalCase = patientService.createCase(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(medicalCase, "Case submitted successfully"));
    }

    @GetMapping("/cases")
    public ResponseEntity<ApiResponse<List<Case>>> getMyCases(
            @RequestHeader("X-User-Id") Long userId) {
        List<Case> cases = patientService.getPatientCases(userId);
        return ResponseEntity.ok(ApiResponse.success(cases));
    }

    @GetMapping("/cases/doctor/{doctorId}")
    public ResponseEntity<ApiResponse<List<CaseDto>>> getCasesForDoctor(
            @PathVariable("doctorId") Long doctorId) {
        List<CaseDto> cases = patientService.getCasesforDoctor(doctorId);
        return ResponseEntity.ok(ApiResponse.success(cases));
    }

    @PostMapping("/cases/{caseId}/accept-appointment")
    public ResponseEntity<ApiResponse<Void>> acceptAppointment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long caseId) {
        patientService.acceptAppointment(userId, caseId);
        return ResponseEntity.ok(ApiResponse.success(null, "Appointment accepted"));
    }

    @PostMapping("/cases/{caseId}/pay")
    public ResponseEntity<ApiResponse<Void>> payConsultationFee(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long caseId,
            @RequestParam BigDecimal amount) {
        patientService.payConsultationFee(userId, caseId, amount);
        return ResponseEntity.ok(ApiResponse.success(null, "Payment completed successfully"));
    }
}