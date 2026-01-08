package com.supervisorservice.controller;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.RescheduleRequestResponseDto;
import com.commonlibrary.entity.AppointmentStatus;
import com.supervisorservice.dto.*;
import com.supervisorservice.service.AppointmentManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for managing appointments on behalf of assigned patients
 * Supervisor can view, accept, and request reschedule for patient appointments
 */
@RestController
@RequestMapping("/api/supervisors/appointments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Supervisor Appointments", description = "Appointment management endpoints for Medical Supervisors")
public class SupervisorAppointmentController {

    private final AppointmentManagementService appointmentService;

    /**
     * Get all appointments for supervisor's assigned patients
     * Supports filtering by patient, status, and date
     */
    @GetMapping
    @Operation(summary = "Get all appointments", 
               description = "Retrieves appointments for all patients assigned to the supervisor")
    public ResponseEntity<ApiResponse<List<SupervisorAppointmentDto>>> getAppointments(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) @Parameter(description = "Filter by patient ID") Long patientId,
            @RequestParam(required = false) @Parameter(description = "Filter by case ID") Long caseId,
            @RequestParam(required = false) @Parameter(description = "Filter by status") AppointmentStatus status,
            @RequestParam(required = false) @Parameter(description = "Filter by specific date") 
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @Parameter(description = "Start date for date range filter")
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @Parameter(description = "End date for date range filter")
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "false") @Parameter(description = "Show only upcoming") 
                Boolean upcomingOnly,
            @RequestParam(required = false, defaultValue = "scheduledTime") @Parameter(description = "Sort field") 
                String sortBy,
            @RequestParam(required = false, defaultValue = "asc") @Parameter(description = "Sort order (asc/desc)") 
                String sortOrder) {

        log.info("Getting appointments for supervisor userId: {} with filters", userId);

        AppointmentFilterDto filter = AppointmentFilterDto.builder()
                .patientId(patientId)
                .caseId(caseId)
                .status(status)
                .date(date)
                .startDate(startDate)
                .endDate(endDate)
                .upcomingOnly(upcomingOnly)
                .sortBy(sortBy)
                .sortOrder(sortOrder)
                .build();

        List<SupervisorAppointmentDto> appointments = appointmentService.getAppointments(userId, filter);
        return ResponseEntity.ok(ApiResponse.success(appointments));
    }

    /**
     * Get upcoming appointments for supervisor's patients
     */
    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming appointments", 
               description = "Retrieves all upcoming appointments for assigned patients")
    public ResponseEntity<ApiResponse<List<SupervisorAppointmentDto>>> getUpcomingAppointments(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Getting upcoming appointments for supervisor userId: {}", userId);
        
        List<SupervisorAppointmentDto> appointments = appointmentService.getUpcomingAppointments(userId);
        return ResponseEntity.ok(ApiResponse.success(appointments));
    }

    /**
     * Get appointment details by ID
     */
    @GetMapping("/{appointmentId}")
    @Operation(summary = "Get appointment details", 
               description = "Retrieves detailed information about a specific appointment")
    public ResponseEntity<ApiResponse<SupervisorAppointmentDto>> getAppointmentDetails(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable @Parameter(description = "Appointment ID") Long appointmentId) {

        log.info("Getting appointment {} details for supervisor userId: {}", appointmentId, userId);
        
        SupervisorAppointmentDto appointment = appointmentService.getAppointmentDetails(userId, appointmentId);
        return ResponseEntity.ok(ApiResponse.success(appointment));
    }

    /**
     * Get appointments for a specific patient
     */
    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get patient appointments", 
               description = "Retrieves all appointments for a specific assigned patient")
    public ResponseEntity<ApiResponse<List<SupervisorAppointmentDto>>> getPatientAppointments(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable @Parameter(description = "Patient ID") Long patientId) {

        log.info("Getting appointments for patient {} by supervisor userId: {}", patientId, userId);
        
        List<SupervisorAppointmentDto> appointments = appointmentService.getPatientAppointments(userId, patientId);
        return ResponseEntity.ok(ApiResponse.success(appointments));
    }

    /**
     * Get appointments for a specific case
     */
    @GetMapping("/case/{caseId}")
    @Operation(summary = "Get case appointments", 
               description = "Retrieves all appointments associated with a specific case")
    public ResponseEntity<ApiResponse<List<SupervisorAppointmentDto>>> getCaseAppointments(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable @Parameter(description = "Case ID") Long caseId) {

        log.info("Getting appointments for case {} by supervisor userId: {}", caseId, userId);
        
        List<SupervisorAppointmentDto> appointments = appointmentService.getCaseAppointments(userId, caseId);
        return ResponseEntity.ok(ApiResponse.success(appointments));
    }

    /**
     * Accept appointment on behalf of patient
     * This moves the case to payment pending status
     */
    @PostMapping("/accept")
    @Operation(summary = "Accept appointment", 
               description = "Accepts a scheduled appointment on behalf of the patient")
    public ResponseEntity<ApiResponse<Void>> acceptAppointment(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AcceptAppointmentDto dto) {

        log.info("Accepting appointment for case {} patient {} by supervisor userId: {}", 
                dto.getCaseId(), dto.getPatientId(), userId);
        
        appointmentService.acceptAppointment(userId, dto);
        return ResponseEntity.ok(ApiResponse.success(null, "Appointment accepted successfully"));
    }

    /**
     * Create reschedule request on behalf of patient
     */
    @PostMapping("/reschedule-request")
    @Operation(summary = "Create reschedule request", 
               description = "Creates a reschedule request for an appointment on behalf of the patient")
    public ResponseEntity<ApiResponse<RescheduleRequestResponseDto>> createRescheduleRequest(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody SupervisorRescheduleRequestDto dto) {

        log.info("Creating reschedule request for appointment {} by supervisor userId: {}", 
                dto.getAppointmentId(), userId);
        
        RescheduleRequestResponseDto response = appointmentService.createRescheduleRequest(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Reschedule request created successfully"));
    }

    /**
     * Get reschedule requests for supervisor's patients
     */
    @GetMapping("/reschedule-requests")
    @Operation(summary = "Get reschedule requests", 
               description = "Retrieves all reschedule requests for assigned patients")
    public ResponseEntity<ApiResponse<List<RescheduleRequestResponseDto>>> getRescheduleRequests(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) @Parameter(description = "Filter by patient ID") Long patientId) {

        log.info("Getting reschedule requests for supervisor userId: {}", userId);
        
        List<RescheduleRequestResponseDto> requests = appointmentService.getRescheduleRequests(userId, patientId);
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    /**
     * Get reschedule requests for a specific case
     */
    @GetMapping("/case/{caseId}/reschedule-requests")
    @Operation(summary = "Get case reschedule requests", 
               description = "Retrieves reschedule requests for a specific case")
    public ResponseEntity<ApiResponse<List<RescheduleRequestResponseDto>>> getCaseRescheduleRequests(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable @Parameter(description = "Case ID") Long caseId) {

        log.info("Getting reschedule requests for case {} by supervisor userId: {}", caseId, userId);
        
        // Get case appointments first to validate access, then filter reschedule requests
        List<SupervisorAppointmentDto> caseAppointments = appointmentService.getCaseAppointments(userId, caseId);
        
        if (!caseAppointments.isEmpty()) {
            Long patientId = caseAppointments.get(0).getPatientId();
            List<RescheduleRequestResponseDto> allRequests = appointmentService.getRescheduleRequests(userId, patientId);
            
            // Filter to only this case's requests
            List<RescheduleRequestResponseDto> caseRequests = allRequests.stream()
                    .filter(r -> caseId.equals(r.getCaseId()))
                    .toList();
            
            return ResponseEntity.ok(ApiResponse.success(caseRequests));
        }
        
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    /**
     * Get appointment summary statistics
     */
    @GetMapping("/summary")
    @Operation(summary = "Get appointment summary", 
               description = "Retrieves appointment statistics and summary for supervisor's patients")
    public ResponseEntity<ApiResponse<AppointmentSummaryDto>> getAppointmentSummary(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Getting appointment summary for supervisor userId: {}", userId);
        
        AppointmentSummaryDto summary = appointmentService.getAppointmentSummary(userId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * Get today's appointments
     */
    @GetMapping("/today")
    @Operation(summary = "Get today's appointments", 
               description = "Retrieves all appointments scheduled for today")
    public ResponseEntity<ApiResponse<List<SupervisorAppointmentDto>>> getTodayAppointments(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Getting today's appointments for supervisor userId: {}", userId);
        
        AppointmentFilterDto filter = AppointmentFilterDto.builder()
                .date(LocalDate.now())
                .sortBy("scheduledTime")
                .sortOrder("asc")
                .build();
        
        List<SupervisorAppointmentDto> appointments = appointmentService.getAppointments(userId, filter);
        return ResponseEntity.ok(ApiResponse.success(appointments));
    }

    /**
     * Get appointments by status
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "Get appointments by status", 
               description = "Retrieves appointments filtered by status")
    public ResponseEntity<ApiResponse<List<SupervisorAppointmentDto>>> getAppointmentsByStatus(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable @Parameter(description = "Appointment status") AppointmentStatus status) {

        log.info("Getting appointments with status {} for supervisor userId: {}", status, userId);
        
        AppointmentFilterDto filter = AppointmentFilterDto.builder()
                .status(status)
                .sortBy("scheduledTime")
                .sortOrder("asc")
                .build();
        
        List<SupervisorAppointmentDto> appointments = appointmentService.getAppointments(userId, filter);
        return ResponseEntity.ok(ApiResponse.success(appointments));
    }
}