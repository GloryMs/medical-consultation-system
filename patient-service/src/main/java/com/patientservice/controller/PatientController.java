package com.patientservice.controller;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.AppointmentDto;
import com.commonlibrary.dto.ComplaintDto;
import com.commonlibrary.dto.NotificationDto;
import com.commonlibrary.exception.BusinessException;
import com.patientservice.dto.*;
import com.patientservice.entity.Case;
import com.patientservice.feign.ComplaintServiceClient;
import com.patientservice.repository.CaseAssignmentRepository;
import com.patientservice.service.PatientService;
import com.patientservice.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final ReportService reportService;
    private final CaseAssignmentRepository assignmentRepository;
    private final ComplaintServiceClient complaintServiceClient;

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
    public ResponseEntity<ApiResponse<Void>> submitComplaint(
            @Valid @RequestBody ComplaintDto dto) {
        boolean success = false;
        success = complaintServiceClient.submitComplaint(dto).getBody().isSuccess();
        if( success){
            return ResponseEntity.ok(ApiResponse.success(null, "Complaint submitted"));
        }
        return ResponseEntity.ok(ApiResponse.error("Something went wrong while submitting the complaint .."));
    }

    // 8. View My Complaints - MISSING ENDPOINT
    @GetMapping("/complaints")
    public ResponseEntity<ApiResponse<List<ComplaintDto>>> getMyComplaints(
            @RequestHeader("X-User-Id") Long userId) {
        List<ComplaintDto> complaints = complaintServiceClient.getPatientComplaintsById(userId).getBody().getData();
        return ResponseEntity.ok(ApiResponse.success(complaints));
    }

//    @GetMapping("/complaints/{complaintId}")
//    public ResponseEntity<ApiResponse<Complaint>> getComplaintByI( @PathVariable Long complaintId ) {
//         Complaint complaint = complaintRepository.findById(complaintId).orElseThrow(() ->
//                 new BusinessException("No complaints found", HttpStatus.NOT_FOUND));
//         return ResponseEntity.ok(ApiResponse.success(complaint));
//    }

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
    public ResponseEntity<ApiResponse<List<CaseDto>>> getMyCases(
            @RequestHeader("X-User-Id") Long userId) {
        List<CaseDto> cases = patientService.getPatientCases(userId);
        return ResponseEntity.ok(ApiResponse.success(cases));
    }

    @GetMapping("/cases/pool")
    public ResponseEntity<ApiResponse<List<CaseDto>>> getCassesPool(
            @RequestParam String specialization ) {
        List<CaseDto> cases = patientService.getCasesPool(specialization);
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

    @GetMapping("/cases/appointments")
    public ResponseEntity<ApiResponse<List<AppointmentDto>>> getAppointments(
            @RequestHeader("X-User-Id") Long userId){
        List<AppointmentDto> appointments = new ArrayList<>();
        appointments = patientService.getPatientAppointments(userId);
        return ResponseEntity.ok(ApiResponse.success(appointments));
    }

    @PostMapping("/cases/{caseId}/pay")
    public ResponseEntity<ApiResponse<Void>> payConsultationFee(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long caseId,
            @RequestParam BigDecimal amount) {
        patientService.payConsultationFee(userId, caseId, amount);
        return ResponseEntity.ok(ApiResponse.success(null, "Payment completed successfully"));
    }

    @PostMapping("/case-assignment/{doctorId}/assignment/{assignmentId}")
    public ResponseEntity<ApiResponse<Void>> acceptAssignment(
            @PathVariable Long doctorId,
            @PathVariable Long assignmentId) {
        patientService.acceptAssignment(doctorId, assignmentId);
        return ResponseEntity.ok(ApiResponse.success(null, "Assignment accepted"));
    }

    @PostMapping("/case-assignment/{doctorId}/assignment/{assignmentId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectAssignment(
            @PathVariable Long doctorId,
            @PathVariable Long assignmentId,
            @RequestParam String reason) {
        patientService.rejectAssignment(doctorId, assignmentId, reason);
        return ResponseEntity.ok(ApiResponse.success(null, "Assignment rejected"));
    }

    @PostMapping("/cases/{caseId}/claim")
    public ResponseEntity<ApiResponse<Void>> claimCase(
            @PathVariable Long caseId,
            @RequestParam Long doctorId,
            @RequestParam String reason) {
        patientService.claimCase(caseId, doctorId, reason);
        return ResponseEntity.ok(ApiResponse.success(null, "Case claimed"));
    }

    @GetMapping("/case-assignments")
    public ResponseEntity<ApiResponse<List<CaseAssignmentDto>>> getCasesByDoctorIdAndStatus(
            @RequestParam Long doctorId, @RequestParam String status) {
        List<CaseAssignmentDto> assignments = new ArrayList<>();
        if( status.equals("NA") ){
            assignments = assignmentRepository.findByDoctorId(doctorId)
                    .stream().map(PatientService::assignmentDtoCovert).collect(Collectors.toList());
        }
        else
            assignments = assignmentRepository.findByDoctorIdAndStatus(doctorId, status)
                .stream().map(PatientService::assignmentDtoCovert).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(assignments));
    }

    @GetMapping("/cases/all-metrics")
    public ResponseEntity<ApiResponse<Map<String,Long>>> getAllMetrics(){
        Map<String,Long> metrics = new HashMap<>();
        metrics = patientService.getAllCassesMetrics();
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    @GetMapping("/{patientId}/dashboard")
    public ResponseEntity<ApiResponse<PatientDashboardDto>> getPatientDashboard(@PathVariable Long patientId){
        PatientDashboardDto patientDashboardDto = new PatientDashboardDto();
        patientDashboardDto = patientService.getPatientDashboard(patientId);
        return ResponseEntity.ok(ApiResponse.success(patientDashboardDto));
    }

    @GetMapping ("/notifications/{patientId}")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getNotification(@PathVariable Long patientId){
        List<NotificationDto> notificationsDto = new ArrayList<>();
        notificationsDto = patientService.getMyNotifications(patientId);
        return ResponseEntity.ok(ApiResponse.success(notificationsDto));
    }

}