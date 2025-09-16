package com.doctorservice.controller;

import com.commonlibrary.dto.*;
import com.commonlibrary.dto.DoctorDto;
import com.commonlibrary.entity.AssignmentStatus;
import com.commonlibrary.entity.CaseStatus;
import com.commonlibrary.entity.VerificationStatus;
import com.commonlibrary.exception.BusinessException;
import com.doctorservice.dto.*;
import com.doctorservice.entity.*;
import com.doctorservice.feign.PatientServiceClient;
import com.doctorservice.repository.AppointmentRepository;
import com.doctorservice.repository.DoctorRepository;
import com.doctorservice.service.DoctorService;
import com.doctorservice.service.InternalDoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;
    private final DoctorRepository doctorRepository;
    private final PatientServiceClient caseAssignmentRepo;
    private final AppointmentRepository appointmentRepository;
    private final InternalDoctorService internalDoctorService;
    //private final NotEmptyValidatorForCollection notEmptyValidatorForCollection;

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
    public ResponseEntity<ApiResponse<List<AppointmentDto>>> getAppointments(
            @RequestHeader("X-User-Id") Long userId) {
        List<AppointmentDto> appointments = doctorService.getDoctorAppointments(userId);
        return ResponseEntity.ok(ApiResponse.success(appointments));
    }

    @GetMapping("/appointments/{patientId}")
    public ResponseEntity<ApiResponse<List<AppointmentDto>>> getPatientAppointments(
            @PathVariable Long patientId) {
        List<AppointmentDto> appointments = doctorService.getPatientAppointments(patientId);
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
    @GetMapping("/profile/{doctorId}")
    public ResponseEntity<ApiResponse<DoctorProfileDto>> getProfile(
            @PathVariable Long doctorId) {
        DoctorProfileDto profile = doctorService.getProfile(doctorId);
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
            @RequestParam String specialization) {
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

    @PutMapping("/appointments/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmAppointment(
            @RequestParam Long caseId, @RequestParam Long patientId, @RequestParam Long doctorId) {
        doctorService.confirmAppointment(caseId, patientId, doctorId);
        return ResponseEntity.ok(ApiResponse.success(null, "Appointment confirmed"));
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

}
