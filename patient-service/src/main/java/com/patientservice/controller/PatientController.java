package com.patientservice.controller;

import com.commonlibrary.dto.*;
import com.commonlibrary.entity.AssignmentStatus;
import com.commonlibrary.entity.UserType;
import com.commonlibrary.exception.BusinessException;
import com.patientservice.dto.*;
import com.patientservice.entity.Case;
import com.patientservice.feign.AdminServiceClient;
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

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final ReportService reportService;
    private final CaseAssignmentRepository assignmentRepository;
    private final AdminServiceClient adminServiceClient;
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

        log.info("POST /api/patients/cases/{}/reschedule-request - Patient [userId={}]", caseId, userId);

        patientService.requestReschedule(userId, caseId, dto);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Reschedule request sent successfully to doctor")
        );
    }

    @GetMapping("/cases/{caseId}/reschedule-requests")
    public ResponseEntity<ApiResponse<List<RescheduleRequestResponseDto>>> getRescheduleRequests(
            //@RequestHeader("X-User-Id") Long userId,
            @PathVariable Long caseId) {

        log.info("GET /api/patients/cases/{}/reschedule-requests", caseId);

        List<RescheduleRequestResponseDto> requests = patientService.getRescheduleRequests(caseId);

        return ResponseEntity.ok(
                ApiResponse.success(requests)
        );
    }

    @GetMapping("/reschedule-requests/pending")
    public ResponseEntity<ApiResponse<List<RescheduleRequestResponseDto>>> getPendingRescheduleRequests(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("GET /api/patients/reschedule-requests/pending - Patient [userId={}]", userId);

        List<RescheduleRequestResponseDto> pendingRequests = patientService.getPendingRescheduleRequests(userId);

        return ResponseEntity.ok(
                ApiResponse.success(pendingRequests)
        );
    }

    @PutMapping("/reschedule-request/{requestId}/update")
    public ResponseEntity<ApiResponse<Void>> updateRescheduleRequest(
            @PathVariable Long requestId, @RequestParam String status) {

        log.info("PUT /reschedule-request/{}/update", requestId);

        patientService.updateRescheduleRequestStatus( requestId, status);

        return ResponseEntity.ok(ApiResponse.success(null,"Reschedule request status updated successfully")
        );
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
        success = adminServiceClient.submitComplaint(dto).getBody().isSuccess();
        if( success){
            return ResponseEntity.ok(ApiResponse.success(null, "Complaint submitted"));
        }
        return ResponseEntity.ok(ApiResponse.error("Something went wrong while submitting the complaint ..", HttpStatus.BAD_REQUEST));
    }

    // 8. View My Complaints - MISSING ENDPOINT
    @GetMapping("/complaints")
    public ResponseEntity<ApiResponse<List<ComplaintDto>>> getMyComplaints(
            @RequestHeader("X-User-Id") Long userId) {
        List<ComplaintDto> complaints = adminServiceClient.getPatientComplaintsById(userId).getBody().getData();
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
            log.info("Start Calling: createCase endpoint");
            log.info("User ID: {}", userId);
            log.info("Case Title: {}", caseTitle);
            log.info("Dependent ID: {}", dependentId);  // Can now be null safely

            // Validate required fields
            if (userId == null || userId <= 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User ID is required and must be valid", HttpStatus.BAD_REQUEST));
            }

            if (caseTitle == null || caseTitle.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Case title is required", HttpStatus.BAD_REQUEST));
            }

            if (requiredSpecialization == null || requiredSpecialization.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Required specialization is mandatory", HttpStatus.BAD_REQUEST));
            }

            // Dependent ID is optional - can be null (for primary user's own case)
            // If dependent ID is provided, validate it's a positive number
            if (dependentId != null && dependentId <= 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Dependent ID must be a positive number or null", HttpStatus.BAD_REQUEST));
            }

            log.info("Building Create Case DTO...");
            // Build DTO from request parameters
            CreateCaseDto dto = dtoBuilder.buildCreateCaseDto(
                    caseTitle, description, primaryDiseaseCode, secondaryDiseaseCodes,
                    symptomCodes, currentMedicationCodes, requiredSpecialization,
                    secondarySpecializations, urgencyLevel, complexity,
                    requiresSecondOpinion, minDoctorsRequired, maxDoctorsAllowed, dependentId, files);

            log.info("Create Case DTO has built successfully {}", dto.toString());
            Case medicalCase = patientService.createCase(userId, dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(medicalCase, "Case submitted successfully with files"));

        } catch (IllegalArgumentException e) {
            log.warn("Validation error creating case for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), HttpStatus.BAD_REQUEST));

        } catch (RuntimeException e) {
            log.error("Runtime error creating case for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create case: " + e.getMessage(), HttpStatus.BAD_REQUEST));

        } catch (Exception e) {
            log.error("Unexpected error creating case for user {}: {}", userId, e.getMessage(), e);
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create case: An unexpected error occurred", HttpStatus.BAD_REQUEST));
        }
    }

    /**
     * JSON-based case creation endpoint for supervisor-service and other internal services
     * Files can be uploaded separately via /cases/{caseId}/attachments endpoint
     */
    @PostMapping(value = "/cases/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CaseDto>> createCaseJson(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateCaseDto dto) {

        try {
            log.info("JSON endpoint - Creating case for user: {}", userId);
            log.info("Case Title: {}", dto.getCaseTitle());
            log.info("Required Specialization: {}", dto.getRequiredSpecialization());

            // Validate required fields
            if (userId == null || userId <= 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User ID is required and must be valid", HttpStatus.BAD_REQUEST));
            }

            // Set files to empty list for JSON endpoint (files uploaded separately)
            dto.setFiles(new java.util.ArrayList<>());

            // Create case using the existing service method
            Case medicalCase = patientService.createCase(userId, dto);

            // Convert Case entity to CaseDto
            CaseDto caseDto = convertToCaseDto(medicalCase);

            log.info("Case created successfully via JSON endpoint - caseId: {}", medicalCase.getId());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(caseDto, "Case created successfully. Files can be uploaded via /cases/" + medicalCase.getId() + "/attachments"));

        } catch (BusinessException e) {
            log.error("Business error creating case via JSON for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(e.getStatus())
                    .body(ApiResponse.error(e.getMessage(), e.getStatus()));

        } catch (IllegalArgumentException e) {
            log.warn("Validation error creating case via JSON for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), HttpStatus.BAD_REQUEST));

        } catch (Exception e) {
            log.error("Unexpected error creating case via JSON for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create case: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
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

    @GetMapping("/cases/metrics")
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

//    @GetMapping ("/notifications/{patientId}")
//    public ResponseEntity<ApiResponse<List<NotificationDto>>> getNotification(@PathVariable Long patientId){
//        List<NotificationDto> notificationsDto = new ArrayList<>();
//        notificationsDto = patientService.getMyNotifications(patientId);
//        return ResponseEntity.ok(ApiResponse.success(notificationsDto));
//    }
//
//    @PutMapping("/notifications/{notificationId}/{userId}/read")
//    public ResponseEntity<ApiResponse<Void>> markAsRead(
//            @PathVariable Long notificationId,
//            @PathVariable Long userId){
//        notificationServiceClient.markAsRead(notificationId, userId);
//        return ResponseEntity.ok(ApiResponse.success(null, "Marked as read"));
//    }
//
//    @PutMapping("/notifications/{userId}/read-all")
//    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
//            @PathVariable  Long userId) {
//        notificationServiceClient.markAllAsRead(userId);
//        return ResponseEntity.ok(ApiResponse.success(null, "All notifications marked as read"));
//    }


    // ==================== NOTIFICATION ENDPOINTS ====================

    /**
     * Get all notifications for a patient
     * Frontend calls: /api/patients/{userId}/notifications
     */
    @GetMapping("/{userId}/notifications")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getPatientNotifications(
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long requestUserId) {

        // Security check: ensure user can only access their own notifications
        if (!userId.equals(requestUserId)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Unauthorized access to notifications", HttpStatus.BAD_REQUEST));
        }

        log.info("Fetching notifications for patient userId: {}", userId);
        List<NotificationDto> notifications = notificationServiceClient.getUserNotifications(userId, UserType.PATIENT).getData();
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    /**
     * Get unread notifications for a patient
     * Frontend calls: /api/patients/{userId}/notifications/unread
     */
    @GetMapping("/{userId}/notifications/unread")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getUnreadNotifications(
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long requestUserId) {

        if (!userId.equals(requestUserId)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Unauthorized access",HttpStatus.BAD_REQUEST));
        }

        log.info("Fetching unread notifications for patient userId: {}", userId);
        List<NotificationDto> notifications = notificationServiceClient.getUnreadNotifications(userId, UserType.PATIENT).getData();
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    /**
     * Mark a specific notification as read
     * Frontend calls: PUT /api/patients/notifications/{notificationId}/read
     */
    @PutMapping("/notifications/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markNotificationAsRead(
            @PathVariable Long notificationId,
            @RequestParam Long userId,
            @RequestHeader("X-User-Id") Long requestUserId) {

        if (!userId.equals(requestUserId)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Unauthorized", HttpStatus.BAD_REQUEST));
        }

        log.info("Marking notification {} as read for patient userId: {}", notificationId, userId);
        notificationServiceClient.markAsRead(notificationId, userId, UserType.PATIENT);
        return ResponseEntity.ok(ApiResponse.success(null, "Notification marked as read"));
    }

    /**
     * Mark all notifications as read for a patient
     * Frontend calls: PUT /api/patients/{userId}/notifications/read-all
     */
    @PutMapping("/{userId}/notifications/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllNotificationsAsRead(
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long requestUserId) {

        if (!userId.equals(requestUserId)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Unauthorized", HttpStatus.BAD_REQUEST));
        }

        log.info("Marking all notifications as read for patient userId: {}", userId);
        notificationServiceClient.markAllAsRead(userId, UserType.PATIENT);
        return ResponseEntity.ok(ApiResponse.success(null, "All notifications marked as read"));
    }

    /**
     * Get unread notification count
     * Frontend calls: GET /api/patients/{userId}/notifications/count
     */
    @GetMapping("/{userId}/notifications/count")
    public ResponseEntity<ApiResponse<Long>> getUnreadNotificationCount(
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long requestUserId) {

        if (!userId.equals(requestUserId)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Unauthorized", HttpStatus.BAD_REQUEST));
        }

        log.info("Getting unread count for patient userId: {}", userId);
        Long count = notificationServiceClient.getUnreadNotificationCount(userId, UserType.PATIENT).getData();
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * Helper method to convert Case entity to CaseDto
     */
    private CaseDto convertToCaseDto(Case medicalCase) {
        CaseDto dto = new CaseDto();
        dto.setId(medicalCase.getId());
        dto.setPatientId(medicalCase.getPatient().getId());
        dto.setPatientName(medicalCase.getPatient().getFullName());
        dto.setDependantId(medicalCase.getDependent() != null ? medicalCase.getDependent().getId() : null);
        dto.setCaseTitle(medicalCase.getCaseTitle());
        dto.setDescription(medicalCase.getDescription());
        dto.setStatus(medicalCase.getStatus());
        dto.setRequiredSpecialization(medicalCase.getRequiredSpecialization());
        dto.setCreatedAt(medicalCase.getCreatedAt());
        dto.setPrimaryDiseaseCode(medicalCase.getPrimaryDiseaseCode());
        dto.setSecondaryDiseaseCodes(medicalCase.getSecondaryDiseaseCodes());
        dto.setSymptomCodes(medicalCase.getSymptomCodes());
        dto.setCurrentMedicationCodes(medicalCase.getCurrentMedicationCodes());
        dto.setSecondarySpecializations(medicalCase.getSecondarySpecializations());
        dto.setPaymentStatus(medicalCase.getPaymentStatus());
        dto.setComplexity(medicalCase.getComplexity());
        dto.setUrgencyLevel(medicalCase.getUrgencyLevel());
        dto.setRequiresSecondOpinion(medicalCase.getRequiresSecondOpinion());
        dto.setMinDoctorsRequired(medicalCase.getMinDoctorsRequired());
        dto.setMaxDoctorsAllowed(medicalCase.getMaxDoctorsAllowed());
        dto.setSubmittedAt(medicalCase.getSubmittedAt());
        dto.setFirstAssignedAt(medicalCase.getFirstAssignedAt());
        dto.setLastAssignedAt(medicalCase.getLastAssignedAt());
        dto.setClosedAt(medicalCase.getClosedAt());
        dto.setAssignmentAttempts(medicalCase.getAssignmentAttempts());
        dto.setRejectionCount(medicalCase.getRejectionCount());
        dto.setIsDeleted(medicalCase.getIsDeleted());
        dto.setConsultationFee(medicalCase.getConsultationFee());
        dto.setFeeSetAt(medicalCase.getFeeSetAt());
        dto.setMedicalReportFileLink(medicalCase.getMedicalReportFileLink());
        dto.setReportId(medicalCase.getReportId());
        return dto;
    }

    /**
     * Create patient profile by supervisor
     */
    @PostMapping("/create-by-supervisor")
    public ResponseEntity<ApiResponse<PatientProfileDto>> createPatientBySupervisor(
            @Valid @RequestBody CreatePatientProfileRequest request,
            @RequestHeader("X-Supervisor-Id") Long supervisorId) {

        log.info("Creating patient profile by supervisor: {}", supervisorId);

        PatientProfileDto patient = patientService.createPatientBySupervisor(request, supervisorId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(patient, "Patient profile created successfully" ));
    }

    /**
     * Get patient by email
     */
    @GetMapping("/by-email")
    public ResponseEntity<ApiResponse<PatientProfileDto>> getPatientByEmail(
            @RequestParam String email) {

        log.info("Getting patient by email: {}", email);

        PatientProfileDto patient = patientService.getPatientByEmail(email);

        return ResponseEntity.ok(ApiResponse.success(patient));
    }

}