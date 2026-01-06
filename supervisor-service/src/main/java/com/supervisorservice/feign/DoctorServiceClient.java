package com.supervisorservice.feign;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.AppointmentDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign client for doctor-service
 */
@FeignClient(
    name = "doctor-service",
    url = "${doctor-service.url:http://localhost:8083}",
    fallback = DoctorServiceClientFallback.class
)
public interface DoctorServiceClient {

    /**
     * Get appointments for a patient
     */
    @GetMapping("/api/doctors/appointments/{patientId}")
    ApiResponse<List<AppointmentDto>> getPatientAppointments(@PathVariable Long patientId);

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
}