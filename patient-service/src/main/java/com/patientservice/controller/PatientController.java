package com.patientservice.controller;

import com.commonlibrary.dto.*;
import com.commonlibrary.entity.AssignmentStatus;
import com.commonlibrary.exception.BusinessException;
import com.patientservice.dto.*;
import com.patientservice.entity.Case;
import com.patientservice.feign.ComplaintServiceClient;
import com.patientservice.feign.NotificationServiceClient;
import com.patientservice.repository.CaseAssignmentRepository;
import com.patientservice.service.DocumentService;
import com.patientservice.service.PatientService;
import com.patientservice.service.ReportService;
import com.patientservice.util.CreateCaseDtoBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final ReportService reportService;
    private final CaseAssignmentRepository assignmentRepository;
    private final ComplaintServiceClient complaintServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final DocumentService documentService;
    private final CreateCaseDtoBuilder dtoBuilder;

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

    @PutMapping("/cases/{caseId}/delete")
    public ResponseEntity<ApiResponse<Void>> deleteCase(@PathVariable Long caseId,
                                                        @RequestHeader("X-User-Id") Long userId) {
        patientService.deleteCase(caseId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Case deleted - id: " + caseId));
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

//    @PostMapping("/cases")
//    public ResponseEntity<ApiResponse<Case>> createCase(
//            @RequestHeader("X-User-Id") Long userId,
//            @Valid @RequestBody CreateCaseDto dto) {
//        Case medicalCase = patientService.createCase(userId, dto);
//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(ApiResponse.success(medicalCase, "Case submitted successfully"));
//    }

    /**
     * Updated CreateCase endpoint with file upload support
     */
    @PostMapping(value = "/cases", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Case>> createCase(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("caseTitle") String caseTitle,
            @RequestParam("description") String description,
            @RequestParam(value = "primaryDiseaseCode", required = false) String primaryDiseaseCode,
            @RequestParam(value = "secondaryDiseaseCodes", required = false) List<String> secondaryDiseaseCodes,
            @RequestParam(value = "symptomCodes", required = false) List<String> symptomCodes,
            @RequestParam(value = "currentMedicationCodes", required = false) List<String> currentMedicationCodes,
            @RequestParam("requiredSpecialization") String requiredSpecialization,
            @RequestParam(value = "secondarySpecializations", required = false) List<String> secondarySpecializations,
            @RequestParam(value = "urgencyLevel", required = false) String urgencyLevel,
            @RequestParam(value = "complexity", required = false) String complexity,
            @RequestParam(value = "requiresSecondOpinion", required = false) Boolean requiresSecondOpinion,
            @RequestParam(value = "minDoctorsRequired", required = false) Integer minDoctorsRequired,
            @RequestParam(value = "maxDoctorsAllowed", required = false) Integer maxDoctorsAllowed,
            @RequestParam(value = "dependentId", required = false) Long dependentId,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {

        try {
            // Build DTO from request parameters
            CreateCaseDto dto = dtoBuilder.buildCreateCaseDto(
                    caseTitle, description, primaryDiseaseCode, secondaryDiseaseCodes,
                    symptomCodes, currentMedicationCodes, requiredSpecialization,
                    secondarySpecializations, urgencyLevel, complexity,
                    requiresSecondOpinion, minDoctorsRequired, maxDoctorsAllowed, dependentId, files
            );

            Case medicalCase = patientService.createCase(userId, dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(medicalCase, "Case submitted successfully with files"));

        } catch (Exception e) {
            log.error("Error creating case with files for user {}: {}", userId, e.getMessage(), e);
            throw new BusinessException("Failed to create case: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/cases/{caseId}")
    public ResponseEntity<ApiResponse<Void>> updateCase(@PathVariable Long caseId,
                                                        @RequestBody  UpdateCaseDto updatedCase){
        try{
            patientService.updateCase(caseId, updatedCase );
        } catch (Exception e) {
            log.error("Error while updating case# {} : {}", caseId, e.getMessage(), e);
            throw new BusinessException("Failed to update provided case " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Case updated successfully without files"));
    }

    /**
     * Update case attachments - Allow patients to upload additional files to existing case
     */
    @PostMapping(value = "/cases/{caseId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CaseAttachmentsDto>> updateCaseAttachments(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long caseId,
            @RequestParam("files") List<MultipartFile> files) {

        try {
            log.info("Updating attachments for case {} by user {} with {} files", caseId, userId, files.size());

            CaseAttachmentsDto result = patientService.updateCaseAttachments(userId, caseId, files);

            return ResponseEntity.ok(
                    ApiResponse.success(result,
                            String.format("Successfully uploaded %d additional files to case", files.size()))
            );

        } catch (Exception e) {
            log.error("Error updating case attachments for case {} by user {}: {}",
                    caseId, userId, e.getMessage(), e);
            throw new BusinessException("Failed to update case attachments: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get case attachments summary
     */
    @GetMapping("/cases/{caseId}/attachments")
    public ResponseEntity<ApiResponse<CaseAttachmentsDto>> getCaseAttachments(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long caseId) {

        try {
            CaseAttachmentsDto attachments = patientService.getCaseAttachments(userId, caseId);
            return ResponseEntity.ok(ApiResponse.success(attachments));

        } catch (Exception e) {
            log.error("Error retrieving case attachments for case {} by user {}: {}",
                    caseId, userId, e.getMessage(), e);
            throw new BusinessException("Failed to retrieve case attachments: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/cases")
    public ResponseEntity<ApiResponse<List<CaseDto>>> getMyCases(
            @RequestHeader("X-User-Id") Long userId) {
        List<CaseDto> cases = patientService.getPatientCases(userId);
        return ResponseEntity.ok(ApiResponse.success(cases));
    }

    @GetMapping("/cases/doctor/{doctorId}/active")
    public ResponseEntity<ApiResponse<List<CaseDto>>> getDoctorActiveCases(
            @PathVariable("doctorId") Long doctorId){
        List<CaseDto> cases = patientService.getDoctorActiveCases(doctorId);
        return ResponseEntity.ok(ApiResponse.success(cases));
    }

    @GetMapping("/cases/doctor/{doctorId}/completed")
    public ResponseEntity<ApiResponse<List<CaseDto>>> getDoctorCompletedCases(
            @PathVariable("doctorId") Long doctorId){
        List<CaseDto> cases = patientService.getDoctorCompletedCases(doctorId);
        return ResponseEntity.ok(ApiResponse.success(cases));
    }

    //getAllDoctorCases
    @GetMapping("/cases/doctor/{doctorId}/all")
    public ResponseEntity<ApiResponse<List<CaseDto>>> getAllDoctorCases(
            @PathVariable("doctorId") Long doctorId){
        List<CaseDto> cases = patientService.getAllDoctorCases(doctorId);
        return ResponseEntity.ok(ApiResponse.success(cases));
    }

    @GetMapping("/cases/doctor/{doctorId}/closed")
    public ResponseEntity<ApiResponse<List<CaseDto>>> getDoctorClosedCases(
            @PathVariable("doctorId") Long doctorId){
        List<CaseDto> cases = patientService.getDoctorClosedCases(doctorId);
        return ResponseEntity.ok(ApiResponse.success(cases));
    }

    @GetMapping("/cases/pool")
    public ResponseEntity<ApiResponse<List<CaseDto>>> getCassesPool(
            @RequestParam String specialization ) {
        List<CaseDto> cases = patientService.getCasesPool(specialization);
        return ResponseEntity.ok(ApiResponse.success(cases));
    }

    @GetMapping("/cases/doctor/{doctorId}")
    public ResponseEntity<ApiResponse<List<CaseDto>>> getAssignedCasesForDoctor(
            @PathVariable("doctorId") Long doctorId) {
        List<CaseDto> cases = patientService.getAssignedCasesForDoctor(doctorId);
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

    @PostMapping("/cases/pay")
    public ResponseEntity<ApiResponse<Void>> payConsultationFee(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ProcessPaymentDto dto) {
        patientService.payConsultationFee(userId, dto);
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

    @GetMapping("/cases/{caseId}/custom-info")
    ResponseEntity<ApiResponse<CustomPatientDto>> getCustomPatientInfo( @PathVariable Long caseId,
            @RequestParam Long doctorId){
        CustomPatientDto patientDto = new CustomPatientDto();
        patientDto = patientService.getCustomPatientInformation( caseId, doctorId );
        return ResponseEntity.ok(ApiResponse.success(patientDto));
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
            assignments = assignmentRepository.findByDoctorIdAndStatus(doctorId, AssignmentStatus.valueOf(status))
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

    @PutMapping("/notifications/{notificationId}/{userId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long notificationId,
            @PathVariable Long userId){
        notificationServiceClient.markAsRead(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Marked as read"));
    }

    @PutMapping("/notifications/{userId}/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @PathVariable  Long userId) {
        notificationServiceClient.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "All notifications marked as read"));
    }


}