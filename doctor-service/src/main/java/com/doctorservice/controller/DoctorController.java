package com.doctorservice.controller;

import com.commonlibrary.dto.*;
import com.commonlibrary.dto.CaseDto;
import com.commonlibrary.entity.AssignmentStatus;
import com.commonlibrary.entity.CaseStatus;
import com.commonlibrary.entity.VerificationStatus;
import com.commonlibrary.exception.BusinessException;
import com.doctorservice.dto.*;
import com.doctorservice.entity.*;
import com.doctorservice.feign.NotificationServiceClient;
import com.doctorservice.feign.PatientServiceClient;
import com.doctorservice.feign.PaymentServiceClient;
import com.doctorservice.repository.AppointmentRepository;
import com.doctorservice.repository.DoctorRepository;
import com.doctorservice.service.DoctorService;
import com.doctorservice.service.InternalDoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;
    private final DoctorRepository doctorRepository;
    private final PatientServiceClient caseAssignmentRepo;
    private final InternalDoctorService internalDoctorService;
    private final NotificationServiceClient notificationServiceClient;
    private final PaymentServiceClient paymentServiceClient;

    /**
     * Get Doctor Dashboard Data
     * Provides comprehensive dashboard data including stats, recent cases, appointments, and notifications
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DoctorDashboardDto>> getDoctorDashboard(
            @RequestHeader("X-User-Id") Long userId) {
        DoctorDashboardDto dashboardData = doctorService.getDoctorDashboard(userId);
        return ResponseEntity.ok(ApiResponse.success(dashboardData));
    }

    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<DoctorProfileDto>> createProfile(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody DoctorProfileDto dto) {
        DoctorProfileDto profile = doctorService.createProfile(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(profile, "Profile created successfully"));
    }

    // 10. Get Doctor Profile
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<DoctorProfileDto>> getProfile(
            @RequestHeader("X-User-Id") Long userId) {
        DoctorProfileDto profile = doctorService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PostMapping("/{doctorId}/rate")
    public ResponseEntity<ApiResponse<Double>> rateDoctor(
            @RequestHeader("X-User-Id") Long patientId,
            @PathVariable Long doctorId,
            @Valid @RequestBody SubmitRatingDto ratingDto) {

        Double newAverageRating = doctorService.rateDoctor(patientId, doctorId, ratingDto);
        return ResponseEntity.ok(ApiResponse.success(newAverageRating, "Doctor rated successfully"));
    }

    @GetMapping("/doctor/custom-info/{doctorId}")
    public ResponseEntity<ApiResponse<CustomDoctorInfoDto>> getDoctorCustomInfo(
            @PathVariable Long doctorId){
        return ResponseEntity.ok(ApiResponse.success(doctorService.getDoctorCustomInfo(doctorId)));
    }

    /**
     * Update Doctor Profile
     * Updates the doctor's profile information
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<DoctorProfileDto>> updateProfile(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody DoctorProfileDto dto) {
        DoctorProfileDto updatedProfile = doctorService.updateProfile(userId, dto);
        return ResponseEntity.ok(ApiResponse.success(updatedProfile, "Profile updated successfully"));
    }

    /**
     * Set consultation fee for a specific case
     * Called by doctor after accepting assignment when no hourlyRate or caseRate is set
     */
    @PostMapping("/cases/{caseId}/set-fee")
    public ResponseEntity<ApiResponse<Void>> setCaseFee(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long caseId,
            @Valid @RequestBody SetCaseFeeDto dto) {

        doctorService.setCaseFee(userId, caseId, dto.getConsultationFee());
        return ResponseEntity.ok(ApiResponse.success(null, "Consultation fee set successfully"));
    }

    @GetMapping ("/cases/{caseId}/patient/custom")
    public ResponseEntity<ApiResponse<CustomPatientDto>> getCustomPatientInfo(@PathVariable Long caseId,
                        @RequestHeader("X-User-Id") Long userId){
        //
        CustomPatientDto customPatientInfo = doctorService.getCustomPatientInfo(caseId, userId);
        return ResponseEntity.ok(ApiResponse.success(customPatientInfo));
    }

    @PostMapping("/cases/{caseId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptCase(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long caseId) {
        doctorService.acceptCase(userId, caseId);
        return ResponseEntity.ok(ApiResponse.success(null, "Case accepted"));
    }

    @PostMapping("/appointments")
    public ResponseEntity<ApiResponse<Appointment>> scheduleAppointment(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ScheduleAppointmentDto dto) {
        Appointment appointment = doctorService.scheduleAppointment(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(appointment, "Appointment scheduled"));
    }

    @GetMapping("/appointments")
    public ResponseEntity<ApiResponse<List<AppointmentDto>>> getAppointments(
            @RequestHeader("X-User-Id") Long userId){
        List<AppointmentDto> appointments = doctorService.getDoctorAppointments(userId);
        return ResponseEntity.ok(ApiResponse.success(appointments));
    }

    @PutMapping("/appointments/complete")
    public ResponseEntity<ApiResponse<Void>> completeAppointment(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody CompleteAppointmentDto dto){
        doctorService.completeAppointment(userId,  dto);
        return ResponseEntity.ok(ApiResponse.success(null, "Appointment completed"));
    }

    @PutMapping("/appointments/{appointmentId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelAppointment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long appointmentId,
            @RequestParam String reason){
        doctorService.cancelAppointment(userId, appointmentId, reason);
        return ResponseEntity.ok(ApiResponse.success(null, "Appointment completed"));
    }

    /**
     * Get available time slots for scheduling appointments
     *
     * @param date The date to check (format: yyyy-MM-dd)
     * @param duration Optional appointment duration in minutes (default: 30)
     * @return List of available time slots
     */
    @GetMapping("/appointments/available-slots")
    public ResponseEntity<ApiResponse<List<AvailableSlotDto>>> getAvailableTimeSlots(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false, defaultValue = "30") Integer duration) {

        List<LocalDateTime> availableSlots = doctorService.getAvailableTimeSlots(
                userId,
                date,
                duration
        );

        // Convert to DTO format
        List<AvailableSlotDto> slotDtos = availableSlots.stream()
                .map(slot -> AvailableSlotDto.builder()
                        .startTime(slot)
                        .endTime(slot.plusMinutes(duration))
                        .duration(duration)
                        .available(true)
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(slotDtos, "Available slots retrieved successfully"));
    }

    /**
     * Check if a specific time slot is available
     *
     * @param scheduledTime The proposed appointment time
     * @param duration The appointment duration
     * @return Whether the slot is available
     */
    @GetMapping("/appointments/check-availability")
    public ResponseEntity<ApiResponse<SlotAvailabilityDto>> checkSlotAvailability(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledTime,
            @RequestParam(required = false, defaultValue = "30") Integer duration) {

        boolean isAvailable = doctorService.isTimeSlotAvailable(
                userId,
                scheduledTime,
                duration,
                null
        );

        SlotAvailabilityDto response = SlotAvailabilityDto.builder()
                .scheduledTime(scheduledTime)
                .duration(duration)
                .available(isAvailable)
                .message(isAvailable ? "Time slot is available" : "Time slot conflicts with existing appointment")
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "Availability checked"));
    }

    @GetMapping("/appointments/{patientId}")
    public ResponseEntity<ApiResponse<List<AppointmentDto>>> getPatientAppointments(
            @PathVariable Long patientId) {
        List<AppointmentDto> appointments = doctorService.getPatientAppointments(patientId);
        return ResponseEntity.ok(ApiResponse.success(appointments));
    }

    @GetMapping("/consultation-reports")
    public ResponseEntity<ApiResponse<List<ConsultationReportDto>>> getConsultationReports(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) ReportStatus status) {
        List<ConsultationReportDto> reports;
        if (status == null) {
            reports = doctorService.getConsultationReports(userId);
        }else {
            reports = doctorService.getConsultationReportsByStatus(userId, status);
        }
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    /**
     * Create consultation report (DRAFT status)
     */
    @PostMapping("/consultation-reports/create")
    public ResponseEntity<ApiResponse<ConsultationReportDto>> createReport(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ConsultationReportDto dto) {

        ConsultationReportDto report = doctorService.createConsultationReport(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(report, "Report created successfully as DRAFT"));
    }

    @GetMapping("/consultation-reports/{caseId}/custom-case")
    public ResponseEntity<ApiResponse<CaseDataForMedicalReportDto>> getCaseDetailsForMedicalReport(
            @RequestHeader("X-User-Id") Long userId, @PathVariable Long caseId){
        CaseDataForMedicalReportDto caseData = doctorService.getCaseDetailsForMedicalReport(userId, caseId);
        return ResponseEntity.ok(ApiResponse.success(caseData));
    }

    /**
     * Get single consultation report by ID
     */
    @GetMapping("/consultation-reports/{reportId}")
    public ResponseEntity<ApiResponse<ConsultationReportDto>> getConsultationReport(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long reportId) {
        ConsultationReportDto report = doctorService.getConsultationReportById(userId, reportId);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    /**
     * Update consultation report (only DRAFT reports)
     */
    @PutMapping("/consultation-reports/{reportId}")
    public ResponseEntity<ApiResponse<ConsultationReportDto>> updateConsultationReport(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long reportId,
            @Valid @RequestBody UpdateReportDto dto) {

        ConsultationReportDto report = doctorService.updateConsultationReport(userId, reportId, dto);
        return ResponseEntity.ok(ApiResponse.success(report, "Report updated successfully"));
    }

    /**
     * Export report to PDF (finalizes the report)
     */
    @PostMapping("/consultation-reports/{reportId}/export")
    public ResponseEntity<ApiResponse<ExportResponse>> exportReportToPdf(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long reportId) {

        String pdfUrl = doctorService.exportReportToPdf(userId, reportId);

        ExportResponse response = new ExportResponse();
        response.setReportId(reportId);
        response.setPdfUrl(pdfUrl);
        response.setMessage("Report exported successfully and finalized");

        return ResponseEntity.ok(ApiResponse.success(response,
                "Report exported to PDF successfully. Report is now finalized and cannot be edited."));
    }

    /**
     * Delete consultation report (only DRAFT reports)
     */
    @DeleteMapping("/consultation-reports/{reportId}")
    public ResponseEntity<ApiResponse<Void>> deleteConsultationReport(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long reportId) {
        doctorService.deleteConsultationReport(userId, reportId);
        return ResponseEntity.ok(ApiResponse.success(null, "Report deleted successfully"));
    }

    // Response DTO for export endpoint
    @lombok.Data
    public static class ExportResponse {
        private Long reportId;
        private String pdfUrl;
        private String message;
    }

//    /**
//     * Get reminders for an appointment (for testing/admin purposes)
//     */
//    @GetMapping("/appointments/{appointmentId}/reminders")
//    public ResponseEntity<List<AppointmentReminderDto>> getAppointmentReminders(
//            @PathVariable Long appointmentId) {
//
//        List<AppointmentReminderDto> reminders =
//                appointmentReminderService.getRemindersForAppointment(appointmentId);
//
//        return ResponseEntity.ok(reminders);
//    }
//
//    /**
//     * Manually trigger reminder send (for testing)
//     */
//    @PostMapping("/reminders/{reminderId}/send")
//    public ResponseEntity<Void> sendReminderManually(
//            @PathVariable Long reminderId) {
//
//        appointmentReminderService.sendReminderManually(reminderId);
//
//        return ResponseEntity.ok().build();
//    }

    // 11. Update Availability
    @PutMapping("/availability")
    public ResponseEntity<ApiResponse<Void>> updateAvailability(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AvailabilityDto dto) {
        doctorService.updateAvailability(userId, dto);
        return ResponseEntity.ok(ApiResponse.success(null, "Availability updated"));
    }

    /**
     * Get current availability status
     */
    @GetMapping("/availability/status")
    public ResponseEntity<ApiResponse<AvailabilityStatusDto>> getAvailabilityStatus(
            @RequestHeader("X-User-Id") Long userId) {
        AvailabilityStatusDto status = doctorService.getAvailabilityStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * Quick toggle availability
     */
    @PostMapping("/availability/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleAvailability(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Boolean isAvailable,
            @RequestParam(required = false) String reason) {
        doctorService.toggleAvailability(userId, isAvailable, reason);
        return ResponseEntity.ok(ApiResponse.success(null, "Availability toggled successfully"));
    }

    // 12. Get Assigned Cases
    @GetMapping("/cases/assigned")
    public ResponseEntity<ApiResponse<List<CaseDto>>> getAssignedCases(
            @RequestHeader("X-User-Id") Long userId) {
        List<CaseDto> cases = doctorService.getAssignedCases(userId);
        return ResponseEntity.ok(ApiResponse.success(cases));
    }

    @GetMapping("/cases/active")
    public ResponseEntity<ApiResponse<List<CaseDto>>> getActiveCases(
            @RequestHeader("X-User-Id") Long userId) {
        List<CaseDto> cases = doctorService.getActiveCases(userId);
        return ResponseEntity.ok(ApiResponse.success(cases));
    }

    @GetMapping("/cases/all")
    public ResponseEntity<ApiResponse<List<CaseDto>>> getAllCases(
            @RequestHeader("X-User-Id") Long userId) {
        List<CaseDto> cases = doctorService.getAllCases(userId);
        return ResponseEntity.ok(ApiResponse.success(cases));
    }

    // 13. Browse Cases Pool
    @GetMapping("/cases/pool")
    public ResponseEntity<ApiResponse<List<CaseDto>>> browseCasesPool(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam String specialization) {
        List<CaseDto> cases = doctorService.browseCasesPool(userId, specialization);
        return ResponseEntity.ok(ApiResponse.success(cases));
    }

    // 15. Reject Case
//    @PostMapping("/cases/{caseId}/reject")
//    public ResponseEntity<ApiResponse<Void>> rejectCase(
//            @RequestHeader("X-User-Id") Long userId,
//            @PathVariable Long caseId,
//            @Valid @RequestBody RejectCaseDto dto) {
//        doctorService.rejectCase(userId, caseId, dto.getReason());
//        return ResponseEntity.ok(ApiResponse.success(null, "Case rejected"));
//    }

    // 16. Set Dynamic Fee for Case
//    @PostMapping("/cases/{caseId}/set-fee")
//    public ResponseEntity<ApiResponse<Void>> setDynamicFee(
//            @RequestHeader("X-User-Id") Long userId,
//            @PathVariable Long caseId,
//            @Valid @RequestBody SetFeeDto dto) {
//        doctorService.setDynamicFee(userId, caseId, dto);
//        return ResponseEntity.ok(ApiResponse.success(null, "Fee set successfully"));
//    }

    // 17. Reschedule Appointment
    @PutMapping("/appointments/{appointmentId}/reschedule")
    public ResponseEntity<ApiResponse<Void>> rescheduleAppointment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long appointmentId,
            @Valid @RequestBody RescheduleAppointmentDto dto) {
        doctorService.rescheduleAppointment(userId, appointmentId, dto);
        return ResponseEntity.ok(ApiResponse.success(null, "Appointment rescheduled"));
    }

    @PostMapping("/reschedule-requests/{caseId}/approve")
    public ResponseEntity<ApiResponse<Appointment>> approveRescheduleRequest(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long caseId,
            @Valid @RequestBody ApproveRescheduleRequestDto dto) {

        Appointment updatedAppointment = doctorService.approveRescheduleRequest(
                userId,
                dto.getAppointmentId(),
                dto.getRescheduleId(),
                caseId,
                dto.getNewScheduledTime(),
                dto.getReason()
        );
        return ResponseEntity.ok(
                ApiResponse.success(updatedAppointment, "Reschedule request approved successfully"));
    }

    @PostMapping("/appointments/{appointmentId}/propose-reschedule")
    public ResponseEntity<ApiResponse<Appointment>> proposeRescheduleTime(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long appointmentId,
            @Valid @RequestBody ProposeRescheduleTimeDto dto) {
        Appointment updatedAppointment = doctorService.proposeRescheduleTime(
                userId,
                appointmentId,
                dto.getProposedTime(),
                dto.getReason()
        );
        return ResponseEntity.ok(
                ApiResponse.success(updatedAppointment,
                        "Reschedule time proposed and notification sent to patient"));
    }

    @PostMapping("/reschedule-requests/{requestId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectRescheduleRequest(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long requestId,
            @Valid @RequestBody RejectRescheduleRequestDto dto) {

        doctorService.rejectRescheduleRequest(userId, requestId, dto.getRejectionReason(),
                dto.getPatientId(), dto.getCaseId(), dto.getDoctorName());
        return ResponseEntity.ok(
                ApiResponse.success(null, "Reschedule request rejected successfully")
        );
    }

    @PutMapping("/appointments/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmAppointment(
            @RequestParam Long caseId, @RequestParam Long patientId, @RequestParam Long doctorId) {
        doctorService.confirmAppointment(caseId, patientId, doctorId);
        return ResponseEntity.ok(ApiResponse.success(null, "Appointment confirmed"));
    }

    // 18. Get Today's Appointments
    @GetMapping("/appointments/today")
    public ResponseEntity<ApiResponse<List<Appointment>>> getTodayAppointments(
            @RequestHeader("X-User-Id") Long userId) {
        List<Appointment> appointments = doctorService.getTodayAppointments(userId);
        return ResponseEntity.ok(ApiResponse.success(appointments));
    }

    // 19. Cancel Appointment
    @PostMapping("/appointments/{appointmentId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelAppointment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long appointmentId,
            @Valid @RequestBody CancelAppointmentDto dto) {
        doctorService.cancelAppointment(userId, appointmentId, dto.getReason());
        return ResponseEntity.ok(ApiResponse.success(null, "Appointment cancelled"));
    }



    // 22. Close Case
    @PostMapping("/cases/{caseId}/close")
    public ResponseEntity<ApiResponse<Void>> closeCase(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long caseId,
            @Valid @RequestBody CloseCaseDto dto) {
        doctorService.closeCase(userId, caseId, dto.getClosureNotes());
        return ResponseEntity.ok(ApiResponse.success(null, "Case closed"));
    }

    // 23. Set Calendar Availability
    @PostMapping("/calendar/availability")
    public ResponseEntity<ApiResponse<CalendarAvailability>> setCalendarAvailability(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CalendarAvailabilityDto dto) {
        CalendarAvailability availability = doctorService.setCalendarAvailability(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(availability, "Availability set"));
    }

    // 24. Block Time Slot
    @PostMapping("/calendar/block")
    public ResponseEntity<ApiResponse<Void>> blockTimeSlot(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody BlockTimeSlotDto dto) {
        doctorService.blockTimeSlot(userId, dto);
        return ResponseEntity.ok(ApiResponse.success(null, "Time slot blocked"));
    }

    // 14. Update Doctor Verification (For admin use via Feign)
    @PutMapping("/{doctorId}/verification")
    public ResponseEntity<ApiResponse<Void>> updateDoctorVerification(
            @PathVariable Long doctorId,
            @RequestParam String status,
            @RequestParam String reason) {
        doctorService.updateVerificationStatus(doctorId, status, reason);
        return ResponseEntity.ok(ApiResponse.success(null, "Verification status updated"));
    }

    /* TODO
        1-After getting the assignments form case_assignments relation table
    *     we must get the case details*/
    @GetMapping("/assignments")
    public ResponseEntity<ApiResponse<List<CaseAssignmentDto>>> getMyAssignments(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) AssignmentStatus status) {

        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        List<CaseAssignmentDto> assignments;
        if (status != null) {
            assignments = caseAssignmentRepo.findByDoctorIdAndStatus(doctor.getId(), status.name()).getBody().getData();
        } else { // "NA case:"
            assignments = caseAssignmentRepo.findByDoctorIdAndStatus(doctor.getId(), "NA").getBody().getData();
        }

        return ResponseEntity.ok(ApiResponse.success(assignments));
    }

    @PostMapping("/assignments/{assignmentId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptAssignment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long assignmentId) {
        doctorRepository.findByUserId(userId).orElseThrow(() ->
                new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));
        caseAssignmentRepo.acceptAssignment(userId, assignmentId);
        return ResponseEntity.ok(ApiResponse.success(null, "Assignment accepted"));
    }

    @PostMapping("/cases/{caseId}/claim")
    public ResponseEntity<ApiResponse<Void>> calimAssignment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long caseId, @RequestParam String note) {
        doctorRepository.findByUserId(userId).orElseThrow(() ->
                new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));
        caseAssignmentRepo.claimCase(caseId, caseId, note);
        return ResponseEntity.ok(ApiResponse.success(null, "Case claimed"));
    }

    @PostMapping("/assignments/{assignmentId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectAssignment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long assignmentId,
            @Valid @RequestBody RejectAssignmentDto dto) {

        doctorRepository.findByUserId(userId).orElseThrow(() ->
                new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));
        caseAssignmentRepo.rejectAssignment(userId, assignmentId, dto.getReason());
        return ResponseEntity.ok(ApiResponse.success(null, "Assignment rejected"));
    }

    @GetMapping("/status/{status}/available/{isAvailable}")
    public ResponseEntity<ApiResponse<List<Doctor>>> findbyVerificationStatusAndIsAvailable(
            @PathVariable VerificationStatus status, @PathVariable Boolean isAvailable){
        List<Doctor> doctors = doctorRepository.findbyVerificationStatusAndIsAvailable(status, isAvailable);
        return ResponseEntity.ok(ApiResponse.success(doctors));
    }

//    @PutMapping("/{doctorId}/update-load/{caseStatus}")
//    public ResponseEntity<ApiResponse<Void>> updateDoctorLoad(@PathVariable Long doctorId,
//                                                       @PathVariable CaseStatus caseStatus,
//                                                              @RequestParam int flag) // 1 -> increase | 0 -> decrease
//    {
//        doctorService.updateDoctorWorkLoad(doctorId, caseStatus, flag);
//        return ResponseEntity.ok(ApiResponse.success(null, "A new case assigned for the doctor: "+ doctorId));
//    }

    @GetMapping("/status/pending-verifications")
    public ResponseEntity<ApiResponse<List<PendingVerificationDto>>> getPendingVerifications(){
        List<PendingVerificationDto> doctors = internalDoctorService.getPendingVerifications();
        return ResponseEntity.ok(ApiResponse.success(doctors));
    }

    @GetMapping("/status/pending-verifications/count")
    public ResponseEntity<ApiResponse<Long>> getPendingVerificationsCount(){
        Long count = doctorRepository.countByVerificationStatus(VerificationStatus.PENDING);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/status/{doctorId}")
    public ResponseEntity<ApiResponse<Doctor>> getDoctorDetails(@PathVariable Long doctorId){
        Doctor doctor = doctorRepository.findByUserId(doctorId).orElse(null);
        return ResponseEntity.ok(ApiResponse.success(doctor));
    }


    @GetMapping("/status/{doctorId}/performance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDoctorPerformance(@PathVariable Long doctorId,
                                                                                 @RequestParam LocalDate startDate,
                                                                                 @RequestParam LocalDate endDate){
        /*TODO
        *  update code below to get doctor performance Map*/
        Map<String, Object> doctorPerformance = new HashMap<>();
        return ResponseEntity.ok(ApiResponse.success(doctorPerformance));
    }

    @GetMapping ("/notifications/{userId}")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getNotification(@PathVariable Long userId){
        List<NotificationDto> notificationsDto = new ArrayList<>();
        notificationsDto = doctorService.getMyNotificationsByUserId(userId);
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

    /**
     * Get doctor payment history with optional filters
     */
    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<List<PaymentHistoryDto>>> getPaymentHistory(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) Map<String, String> filters) {

        Doctor doctor = doctorRepository.findByUserId(userId).orElseThrow(
                () -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));
        List<PaymentHistoryDto> history = doctorService.getPaymentHistory(doctor.getId(), filters);

        return ResponseEntity.ok(ApiResponse.success(history));
    }

    /**
     * Get doctor earnings summary
     */
    @GetMapping("/earnings/summary")
    public ResponseEntity<ApiResponse<DoctorEarningsSummaryDto>> getEarningsSummary(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "month") String period) {

        Doctor doctor = doctorRepository.findByUserId(userId).orElseThrow(
                () -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND)
        );
        DoctorEarningsSummaryDto summary = doctorService.getEarningsSummary(doctor.getId(), period);

        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * Get doctor earnings statistics
     */
    @GetMapping("/earnings/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEarningsStats(
            @RequestHeader("X-User-Id") Long userId) {

        Doctor doctor = doctorRepository.findByUserId(userId).orElseThrow(
                () -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));
        Map<String, Object> stats = doctorService.getEarningsStats(doctor.getId());

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/earnings/export/pdf")
    public ResponseEntity<byte[]> exportEarningsPdf(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        Doctor doctor = doctorRepository.findByUserId(userId).orElseThrow(
                () -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));
        return paymentServiceClient.exportEarningsReportPdf(
                doctor.getId(), period, startDate, endDate
        );
    }

    @GetMapping("/earnings/export/csv")
    public ResponseEntity<String> exportEarningsCsv(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        Doctor doctor = doctorRepository.findByUserId(userId).orElseThrow(
                () -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));
        return paymentServiceClient.exportEarningsReportCsv(
                doctor.getId(), period, startDate, endDate
        );
    }

    @GetMapping("/earnings/chart-data")
    public ResponseEntity<ApiResponse<List<ChartDataPointDto>>> getEarningsChartData(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam String period,
            @RequestParam(defaultValue = "daily") String groupBy) {

        Doctor doctor = doctorRepository.findByUserId(userId).orElseThrow(
                () -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));
        return paymentServiceClient.getEarningsChartData(
                doctor.getId(), period, groupBy
        );
    }

    @GetMapping("/earnings/payment-methods")
    public ResponseEntity<ApiResponse<List<ChartDataPointDto>>> getPaymentMethodDistribution(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam String period) {

        Doctor doctor = doctorRepository.findByUserId(userId).orElseThrow(
                () -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));
        return paymentServiceClient.getPaymentMethodDistribution(
                doctor.getId(), period
        );
    }

    /**
     * Get doctor settings/preferences
     */
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<DoctorSettingsDto>> getSettings(
            @RequestHeader("X-User-Id") Long userId) {
        DoctorSettingsDto settings = doctorService.getSettings(userId);
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    /**
     * Update doctor settings/preferences
     */
    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<DoctorSettingsDto>> updateSettings(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody DoctorSettingsDto dto) {
        DoctorSettingsDto updatedSettings = doctorService.updateSettings(userId, dto);
        return ResponseEntity.ok(ApiResponse.success(updatedSettings, "Settings updated successfully"));
    }

}
