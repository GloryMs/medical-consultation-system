package com.patientservice.controller;

import com.commonlibrary.dto.*;
import com.commonlibrary.entity.CaseStatus;
import com.commonlibrary.exception.BusinessException;
import com.commonlibrary.dto.RescheduleRequestDto;
import com.patientservice.entity.Case;
import com.patientservice.entity.Patient;
import com.patientservice.entity.RescheduleRequest;
import com.patientservice.repository.CaseRepository;
import com.patientservice.repository.PatientRepository;
import com.patientservice.repository.RescheduleRequestRepository;
import com.patientservice.service.CaseAnalyticsService;
import com.patientservice.service.PatientAdminService;
import com.patientservice.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for admin operations on patient cases
 * These endpoints are called by admin-service via Feign
 */
@RestController
@RequestMapping("/api/patients-internal")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Patient Admin Operations", description = "Admin endpoints for patient case management")
public class PatientInternalController {

    private final PatientAdminService patientAdminService;
    private final CaseAnalyticsService caseAnalyticsService;
    private final PatientService patientService;
    private final PatientRepository patientRepository;
    private final CaseRepository caseRepository;
    private final RescheduleRequestRepository rescheduleRequestRepository;


    @GetMapping("/cases/analytics")
    @Operation(summary = "Get comprehensive case analytics")
    public ResponseEntity<ApiResponse<CaseAnalyticsDto>> getCaseAnalytics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        CaseAnalyticsDto analytics = caseAnalyticsService.getCaseAnalytics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(analytics, "Analytics retrieved"));
    }


    /**
     * Get all cases for admin with comprehensive filtering
     * Called by admin-service to retrieve cases for admin panel
     */
    @GetMapping("/cases/admin/all")
    @Operation(summary = "Get all cases for admin", 
               description = "Retrieve all cases with filtering for admin panel")
    public ResponseEntity<ApiResponse<List<CaseDto>>> getAllCasesForAdmin(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String urgencyLevel,
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String searchTerm) {
        
        log.info("Admin fetching all cases with filters - status: {}, urgency: {}, spec: {}", 
                 status, urgencyLevel, specialization);
        
        List<CaseDto> cases = patientAdminService.getAllCasesForAdmin(
                status, urgencyLevel, specialization, patientId, doctorId, 
                startDate, endDate, searchTerm
        );
        
        return ResponseEntity.ok(ApiResponse.success(cases, 
                String.format("Retrieved %d cases", cases.size())));
    }

    /**
     * Get case assignment history
     */
    @GetMapping("/cases/{caseId}/assignments/history")
    @Operation(summary = "Get case assignment history", 
               description = "Retrieve all assignment attempts for a case")
    public ResponseEntity<ApiResponse<List<?>>> getCaseAssignmentHistory(@PathVariable Long caseId) {
        log.info("Fetching assignment history for case {}", caseId);
        
        List<?> assignmentHistory = patientAdminService.getCaseAssignmentHistory(caseId);
        
        return ResponseEntity.ok(ApiResponse.success(assignmentHistory, 
                "Assignment history retrieved successfully"));
    }

    /**
     * Create case assignment (admin initiated)
     */
    @PostMapping("/case-assignments")
    @Operation(summary = "Create case assignment", 
               description = "Create a new case assignment (admin operation)")
    public ResponseEntity<ApiResponse<?>> createCaseAssignment(
            @RequestBody Map<String, Object> assignmentRequest) {
        
        log.info("Creating case assignment: {}", assignmentRequest);
        
        patientAdminService.createCaseAssignment(assignmentRequest);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Assignment created successfully"));
    }

    /**
     * Update assignment status
     */
    @PutMapping("/case-assignments/status")
    @Operation(summary = "Update assignment status", 
               description = "Update the status of a case assignment")
    public ResponseEntity<ApiResponse<?>> updateAssignmentStatus(
            @RequestBody Map<String, Object> updateRequest) {
        
        log.info("Updating assignment status: {}", updateRequest);
        
        patientAdminService.updateAssignmentStatus(updateRequest);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Assignment status updated"));
    }

    /**
     * Update case status (admin override)
     */
    @PutMapping("/cases/{caseId}/status")
    @Operation(summary = "Update case status", 
               description = "Admin override to update case status")
    public ResponseEntity<ApiResponse<?>> updateCaseStatusAdmin(
            @PathVariable Long caseId,
            @RequestBody Map<String, Object> statusUpdate) {
        
        log.info("Admin updating case {} status: {}", caseId, statusUpdate);
        
        patientAdminService.updateCaseStatusAdmin(caseId, statusUpdate);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Case status updated"));
    }

    @GetMapping("/cases/{caseId}")
    public ResponseEntity<ApiResponse<CaseDto>> getCaseDetails(
            @PathVariable Long caseId) {
        CaseDto caseDetails = patientAdminService.getCaseDetails(caseId);
        return ResponseEntity.ok(ApiResponse.success(caseDetails));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<Long>> getPatientUserId(
            @PathVariable Long patientId) {
        Long patientUserId = null;
        patientUserId = patientRepository.findById(patientId).isPresent() ?
                patientRepository.findById(patientId).get().getUserId() : null;
        return ResponseEntity.ok(ApiResponse.success(patientUserId));
    }

    /**
     * Get case metrics for admin dashboard
     */
    @GetMapping("/cases/metrics")
    @Operation(summary = "Get case metrics", 
               description = "Retrieve comprehensive case metrics and statistics")
    public ResponseEntity<ApiResponse<CaseMetricsDto>> getCaseMetrics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        log.info("Fetching case metrics from {} to {}", startDate, endDate);
        
        CaseMetricsDto metrics = patientAdminService.getCaseMetrics(startDate, endDate);
        
        return ResponseEntity.ok(ApiResponse.success(metrics, "Metrics retrieved successfully"));
    }

    /**
     * Get unassigned cases
     */
    @GetMapping("/cases/unassigned")
    @Operation(summary = "Get unassigned cases", 
               description = "Retrieve cases pending doctor assignment")
    public ResponseEntity<ApiResponse<List<CaseDto>>> getUnassignedCases(
            @RequestParam(required = false) String urgencyLevel,
            @RequestParam(required = false) String specialization) {
        
        log.info("Fetching unassigned cases - urgency: {}, spec: {}", urgencyLevel, specialization);
        
        List<CaseDto> cases = patientAdminService.getUnassignedCases(urgencyLevel, specialization);
        
        return ResponseEntity.ok(ApiResponse.success(cases, 
                String.format("Retrieved %d unassigned cases", cases.size())));
    }

    /**
     * Get overdue cases
     */
    @GetMapping("/cases/overdue")
    @Operation(summary = "Get overdue cases", 
               description = "Retrieve cases past their expected completion date")
    public ResponseEntity<ApiResponse<List<CaseDto>>> getOverdueCases() {
        log.info("Fetching overdue cases");
        
        List<CaseDto> cases = patientAdminService.getOverdueCases();
        
        return ResponseEntity.ok(ApiResponse.success(cases, 
                String.format("Retrieved %d overdue cases", cases.size())));
    }

    /**
     * Get patient basic info
     */
    @GetMapping("/{patientId}/basic-info")
    @Operation(summary = "Get patient basic info", 
               description = "Retrieve basic patient information for display")
    public ResponseEntity<ApiResponse<?>> getPatientBasicInfo(@PathVariable Long patientId) {
        log.info("Fetching basic info for patient {}", patientId);
        
        Map<String, Object> patientInfo = patientAdminService.getPatientBasicInfo(patientId);
        
        return ResponseEntity.ok(ApiResponse.success(patientInfo, "Patient info retrieved"));
    }

    /**
     * Get case statistics by status
     */
    @GetMapping("/cases/statistics/by-status")
    @Operation(summary = "Get case statistics by status")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getCaseStatisticsByStatus() {
        log.info("Fetching case statistics by status");
        
        Map<String, Long> statistics = patientAdminService.getCaseStatisticsByStatus();
        
        return ResponseEntity.ok(ApiResponse.success(statistics, "Statistics retrieved"));
    }

    /**
     * Get case statistics by urgency
     */
    @GetMapping("/cases/statistics/by-urgency")
    @Operation(summary = "Get case statistics by urgency level")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getCaseStatisticsByUrgency() {
        log.info("Fetching case statistics by urgency");
        
        Map<String, Long> statistics = patientAdminService.getCaseStatisticsByUrgency();
        
        return ResponseEntity.ok(ApiResponse.success(statistics, "Statistics retrieved"));
    }

    /**
     * Get case statistics by specialization
     */
    @GetMapping("/cases/statistics/by-specialization")
    @Operation(summary = "Get case statistics by specialization")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getCaseStatisticsBySpecialization() {
        log.info("Fetching case statistics by specialization");
        
        Map<String, Long> statistics = patientAdminService.getCaseStatisticsBySpecialization();
        
        return ResponseEntity.ok(ApiResponse.success(statistics, "Statistics retrieved"));
    }


    /**
     * Get patient profile by ID (internal use)
     */
    @GetMapping("/profile/{patientId}")
    public ResponseEntity<ApiResponse<PatientProfileDto>> getPatientById(@PathVariable Long patientId) {
        log.info("Internal: Getting patient profile for ID: {}", patientId);

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        PatientProfileDto dto = patientService.mapToDto(patient);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * Get cases by patient ID (internal use)
     */
    @GetMapping("/cases/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<CaseDto>>> getCasesByPatientId(@PathVariable Long patientId) {
        log.info("Internal: Getting cases for patient ID: {}", patientId);

        List<Case> cases = caseRepository.findByPatientIdAndIsDeletedFalse(patientId);
        List<CaseDto> caseDtos = cases.stream()
                .map(this::mapToCaseDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(caseDtos));
    }

    /**
     * Get reschedule requests by patient ID (internal use)
     */
    @GetMapping("/reschedule-requests/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<RescheduleRequestResponseDto>>> getRescheduleRequestsByPatientId(
            @PathVariable Long patientId) {

        log.info("Internal: Getting reschedule requests for patient ID: {}", patientId);

        // Get all cases for the patient first
        List<Case> patientCases = caseRepository.findByPatientIdAndIsDeletedFalse(patientId);
        List<Long> caseIds = patientCases.stream().map(Case::getId).toList();

        // Get reschedule requests for all patient's cases
        List<RescheduleRequest> requests = caseIds.stream()
                .flatMap(caseId -> rescheduleRequestRepository.findByCaseId(caseId).stream())
                .collect(Collectors.toList());

        List<RescheduleRequestResponseDto> responseDtos = requests.stream()
                .map(this::mapToRescheduleResponseDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responseDtos));
    }

    /**
     * Create reschedule request by supervisor on behalf of patient
     */
    @PostMapping("/reschedule-requests/supervisor")
    public ResponseEntity<ApiResponse<RescheduleRequestResponseDto>> createRescheduleRequestBySupervisor(
            @RequestBody RescheduleRequestDto requestDto,
            @RequestHeader("X-Supervisor-Id") Long supervisorId) {

        log.info("Internal: Creating reschedule request by supervisor {} for case {}",
                supervisorId, requestDto.getCaseId());

        // Validate case exists
        Case medicalCase = caseRepository.findById(requestDto.getCaseId())
                .orElseThrow(() -> new BusinessException("Case not found", HttpStatus.NOT_FOUND));

        // Create reschedule request
        RescheduleRequest request = RescheduleRequest.builder()
                .caseId(requestDto.getCaseId())
                .appointmentId(requestDto.getAppointmentId())
                .patientId(requestDto.getPatientId())
                .preferredTimes( String.join(",", requestDto.getPreferredTimes()))
                .reason(requestDto.getReason())
                .status(com.commonlibrary.entity.RescheduleStatus.PENDING)
                .requestedBy("SUPERVISOR")
                .requestedBySupervisorId(supervisorId)
                .build();

        RescheduleRequest saved = rescheduleRequestRepository.save(request);

        RescheduleRequestResponseDto responseDto = mapToRescheduleResponseDto(saved);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(responseDto, "Reschedule request created successfully"));
    }

    /**
     * Accept appointment by supervisor on behalf of patient
     */
    @PostMapping("/cases/{caseId}/accept-appointment/supervisor")
    public ResponseEntity<ApiResponse<Void>> acceptAppointmentBySupervisor(
            @PathVariable Long caseId,
            @RequestParam Long patientId,
            @RequestHeader("X-Supervisor-Id") Long supervisorId) {

        log.info("Internal: Accepting appointment for case {} by supervisor {}", caseId, supervisorId);

        Case medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new BusinessException("Case not found", HttpStatus.NOT_FOUND));

        // Validate case belongs to patient
        if (!medicalCase.getPatient().getId().equals(patientId)) {
            throw new BusinessException("Case does not belong to patient", HttpStatus.FORBIDDEN);
        }

        // Validate case is in scheduled status
        if (medicalCase.getStatus() != com.commonlibrary.entity.CaseStatus.SCHEDULED) {
            throw new BusinessException("Case is not in scheduled status", HttpStatus.BAD_REQUEST);
        }

        // Move to payment pending status
        medicalCase.setStatus(com.commonlibrary.entity.CaseStatus.PAYMENT_PENDING);
        caseRepository.save(medicalCase);

        log.info("Case {} status updated to PAYMENT_PENDING by supervisor {}", caseId, supervisorId);

        return ResponseEntity.ok(ApiResponse.success(null, "Appointment accepted successfully"));
    }

    /**
     * Pay consultation fee by supervisor
     */
    @PostMapping("/cases/{caseId}/pay-consultation/supervisor")
    public ResponseEntity<ApiResponse<Void>> payConsultationFeeBySupervisor(
            @PathVariable Long caseId,
            @RequestParam Long patientId,
            @RequestBody ProcessPaymentDto paymentDto,
            @RequestHeader("X-Supervisor-Id") Long supervisorId) {

        log.info("Internal: Processing payment for case {} by supervisor {}", caseId, supervisorId);

        // Validate and process payment
        // This would integrate with payment-service
        // For now, update case status

        Case medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new BusinessException("Case not found", HttpStatus.NOT_FOUND));

        if (!medicalCase.getPatient().getId().equals(patientId)) {
            throw new BusinessException("Case does not belong to patient", HttpStatus.FORBIDDEN);
        }

        // Update payment status - actual payment processing would be done via payment-service
        medicalCase.setPaymentStatus(com.commonlibrary.entity.PaymentStatus.COMPLETED);
        medicalCase.setStatus(CaseStatus.SCHEDULED);
        caseRepository.save(medicalCase);

        return ResponseEntity.ok(ApiResponse.success(null, "Payment processed successfully"));
    }

    // ==================== Private Helper Methods ====================

    private CaseDto mapToCaseDto(Case medicalCase) {
        CaseDto dto = new CaseDto();
        dto.setId(medicalCase.getId());
        dto.setPatientId(medicalCase.getPatient().getId());
        dto.setPatientName(medicalCase.getPatient().getFullName());
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
        dto.setSupervisorId(medicalCase.getSubmittedBySupervisorId());
        return dto;
    }

    private RescheduleRequestResponseDto mapToRescheduleResponseDto(RescheduleRequest request) {
        RescheduleRequestResponseDto dto = new RescheduleRequestResponseDto();
        dto.setId(request.getId());
        dto.setCaseId(request.getCaseId());
        dto.setAppointmentId(request.getAppointmentId());
        dto.setPatientId(request.getPatientId());
        dto.setPreferredTimes(request.getPreferredTimes());
        dto.setReason(request.getReason());
        dto.setStatus(request.getStatus().toString());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setUpdatedAt(request.getUpdatedAt());
        return dto;
    }
}