package com.doctorservice.controller;

import com.commonlibrary.dto.ApiResponse;
import com.doctorservice.dto.*;
import com.doctorservice.entity.Appointment;
import com.doctorservice.entity.CalendarAvailability;
import com.doctorservice.entity.ConsultationReport;
import com.doctorservice.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<DoctorProfileDto>> createProfile(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody DoctorProfileDto dto) {
        DoctorProfileDto profile = doctorService.createProfile(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(profile, "Profile created successfully"));
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
    public ResponseEntity<ApiResponse<List<Appointment>>> getAppointments(
            @RequestHeader("X-User-Id") Long userId) {
        List<Appointment> appointments = doctorService.getDoctorAppointments(userId);
        return ResponseEntity.ok(ApiResponse.success(appointments));
    }

    @PostMapping("/consultation-reports")
    public ResponseEntity<ApiResponse<ConsultationReport>> createReport(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ConsultationReportDto dto) {
        ConsultationReport report = doctorService.createConsultationReport(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(report, "Report created successfully"));
    }

    // 10. Get Doctor Profile - MISSING ENDPOINT
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<DoctorProfileDto>> getProfile(
            @RequestHeader("X-User-Id") Long userId) {
        DoctorProfileDto profile = doctorService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    // 11. Update Availability - MISSING ENDPOINT
    @PutMapping("/availability")
    public ResponseEntity<ApiResponse<Void>> updateAvailability(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AvailabilityDto dto) {
        doctorService.updateAvailability(userId, dto);
        return ResponseEntity.ok(ApiResponse.success(null, "Availability updated"));
    }

    // 12. Get Assigned Cases - MISSING ENDPOINT
    @GetMapping("/cases/assigned")
    public ResponseEntity<ApiResponse<List<CaseDto>>> getAssignedCases(
            @RequestHeader("X-User-Id") Long userId) {
        List<CaseDto> cases = doctorService.getAssignedCases(userId);
        return ResponseEntity.ok(ApiResponse.success(cases));
    }

    // 13. Browse Cases Pool - MISSING ENDPOINT
    @GetMapping("/cases/pool")
    public ResponseEntity<ApiResponse<List<CaseDto>>> browseCasesPool(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String specialization) {
        List<CaseDto> cases = doctorService.browseCasesPool(userId, specialization);
        return ResponseEntity.ok(ApiResponse.success(cases));
    }

    // 15. Reject Case - MISSING ENDPOINT
    @PostMapping("/cases/{caseId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectCase(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long caseId,
            @Valid @RequestBody RejectCaseDto dto) {
        doctorService.rejectCase(userId, caseId, dto.getReason());
        return ResponseEntity.ok(ApiResponse.success(null, "Case rejected"));
    }

    // 16. Set Dynamic Fee for Case - MISSING ENDPOINT
    @PostMapping("/cases/{caseId}/set-fee")
    public ResponseEntity<ApiResponse<Void>> setDynamicFee(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long caseId,
            @Valid @RequestBody SetFeeDto dto) {
        doctorService.setDynamicFee(userId, caseId, dto);
        return ResponseEntity.ok(ApiResponse.success(null, "Fee set successfully"));
    }

    // 17. Reschedule Appointment - MISSING ENDPOINT
    @PutMapping("/appointments/{appointmentId}/reschedule")
    public ResponseEntity<ApiResponse<Void>> rescheduleAppointment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long appointmentId,
            @Valid @RequestBody RescheduleDto dto) {
        doctorService.rescheduleAppointment(userId, appointmentId, dto);
        return ResponseEntity.ok(ApiResponse.success(null, "Appointment rescheduled"));
    }

    // 18. Get Today's Appointments - MISSING ENDPOINT
    @GetMapping("/appointments/today")
    public ResponseEntity<ApiResponse<List<Appointment>>> getTodayAppointments(
            @RequestHeader("X-User-Id") Long userId) {
        List<Appointment> appointments = doctorService.getTodayAppointments(userId);
        return ResponseEntity.ok(ApiResponse.success(appointments));
    }

    // 19. Cancel Appointment - MISSING ENDPOINT
    @PostMapping("/appointments/{appointmentId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelAppointment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long appointmentId,
            @Valid @RequestBody CancelAppointmentDto dto) {
        doctorService.cancelAppointment(userId, appointmentId, dto.getReason());
        return ResponseEntity.ok(ApiResponse.success(null, "Appointment cancelled"));
    }

    // 20. Get Consultation Reports - MISSING ENDPOINT
    @GetMapping("/consultation-reports")
    public ResponseEntity<ApiResponse<List<ConsultationReport>>> getConsultationReports(
            @RequestHeader("X-User-Id") Long userId) {
        List<ConsultationReport> reports = doctorService.getConsultationReports(userId);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    // 21. Update Consultation Report - MISSING ENDPOINT
    @PutMapping("/consultation-reports/{reportId}")
    public ResponseEntity<ApiResponse<ConsultationReport>> updateConsultationReport(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long reportId,
            @Valid @RequestBody UpdateReportDto dto) {
        ConsultationReport report = doctorService.updateConsultationReport(userId, reportId, dto);
        return ResponseEntity.ok(ApiResponse.success(report, "Report updated"));
    }

    // 22. Close Case - MISSING ENDPOINT
    @PostMapping("/cases/{caseId}/close")
    public ResponseEntity<ApiResponse<Void>> closeCase(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long caseId,
            @Valid @RequestBody CloseCaseDto dto) {
        doctorService.closeCase(userId, caseId, dto.getClosureNotes());
        return ResponseEntity.ok(ApiResponse.success(null, "Case closed"));
    }

    // 23. Set Calendar Availability - MISSING ENDPOINT
    @PostMapping("/calendar/availability")
    public ResponseEntity<ApiResponse<CalendarAvailability>> setCalendarAvailability(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CalendarAvailabilityDto dto) {
        CalendarAvailability availability = doctorService.setCalendarAvailability(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(availability, "Availability set"));
    }

    // 24. Block Time Slot - MISSING ENDPOINT
    @PostMapping("/calendar/block")
    public ResponseEntity<ApiResponse<Void>> blockTimeSlot(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody BlockTimeSlotDto dto) {
        doctorService.blockTimeSlot(userId, dto);
        return ResponseEntity.ok(ApiResponse.success(null, "Time slot blocked"));
    }

    // 14. Update Doctor Verification - MISSING ENDPOINT (For admin use via Feign)
    @PutMapping("/{doctorId}/verification")
    public ResponseEntity<ApiResponse<Void>> updateDoctorVerification(
            @PathVariable Long doctorId,
            @RequestParam String status,
            @RequestParam String reason) {
        doctorService.updateVerificationStatus(doctorId, status, reason);
        return ResponseEntity.ok(ApiResponse.success(null, "Verification status updated"));
    }
}
