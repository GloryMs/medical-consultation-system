package com.supervisorservice.feign;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.AppointmentDto;
import com.commonlibrary.entity.AppointmentStatus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign client for doctor-service
 */
@FeignClient(
    name = "doctor-service")
public interface DoctorServiceClient {

    /**
     * Get doctor basic info
     */
    @GetMapping("/api/doctors-internal/{doctorId}/basic-info")
    ApiResponse<Map<String, Object>> getDoctorBasicInfo(@PathVariable Long doctorId);

    /**
     * Get doctor statistics
     */
    @GetMapping("/api/doctors-internal/{doctorId}/statistics")
    ApiResponse<Map<String, Object>> getDoctorStatistics(@PathVariable Long doctorId);

    /**
     * Check if doctor can accept cases
     */
    @GetMapping("/api/doctors-internal/{doctorId}/can-accept-cases")
    ApiResponse<Map<String, Object>> canDoctorAcceptCases(@PathVariable Long doctorId);

    /**
     * Get all appointments for a patient
     * @param patientId Patient ID
     * @return List of appointments
     */
    @GetMapping("/api/doctors/appointments/{patientId}")
    ResponseEntity<ApiResponse<List<AppointmentDto>>> getPatientAppointments(
            @PathVariable("patientId") Long patientId);

    /**
     * Get upcoming appointments for a patient
     * @param patientId Patient ID
     * @return List of upcoming appointments
     */
    @GetMapping("/api/doctors/upcoming-appointments/{patientId}")
    ResponseEntity<ApiResponse<List<AppointmentDto>>> getPatientUpcomingAppointments(
            @PathVariable("patientId") Long patientId);

    /**
     * Get appointment by ID
     * @param appointmentId Appointment ID
     * @return Appointment details
     */
    @GetMapping("/api/doctors-internal/appointments/{appointmentId}")
    ResponseEntity<ApiResponse<AppointmentDto>> getAppointmentById(
            @PathVariable("appointmentId") Long appointmentId);

    /**
     * Confirm appointment (triggers payment flow)
     * Called when supervisor confirms appointment on behalf of patient
     */
    @PutMapping("/api/doctors/appointments/confirm")
    ResponseEntity<ApiResponse<Void>> confirmAppointment(
            @RequestParam("caseId") Long caseId,
            @RequestParam("patientId") Long patientId,
            @RequestParam("doctorId") Long doctorId);

    /**
     * Get appointments by case ID
     * @param caseId Case ID
     * @return List of appointments for the case
     */
    @GetMapping("/api/doctors-internal/appointments/case/{caseId}")
    ResponseEntity<ApiResponse<List<AppointmentDto>>> getAppointmentsByCaseId(
            @PathVariable("caseId") Long caseId);

    /**
     * Get appointments filtered by status
     * @param patientId Patient ID
     * @param status Appointment status
     * @return Filtered list of appointments
     */
    @GetMapping("/api/doctors-internal/appointments/patient/{patientId}/status/{status}")
    ResponseEntity<ApiResponse<List<AppointmentDto>>> getPatientAppointmentsByStatus(
            @PathVariable("patientId") Long patientId,
            @PathVariable("status") AppointmentStatus status);
}