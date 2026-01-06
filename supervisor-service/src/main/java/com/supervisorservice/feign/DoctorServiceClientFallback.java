package com.supervisorservice.feign;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.AppointmentDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fallback implementation for DoctorServiceClient
 */
@Component
@Slf4j
public class DoctorServiceClientFallback implements DoctorServiceClient {

    @Override
    public ApiResponse<List<AppointmentDto>> getPatientAppointments(Long patientId) {
        log.error("DoctorService unavailable - getPatientAppointments fallback for patientId: {}", patientId);
        return ApiResponse.error("Doctor service unavailable", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ApiResponse<Map<String, Object>> getDoctorBasicInfo(Long doctorId) {
        log.error("DoctorService unavailable - getDoctorBasicInfo fallback for doctorId: {}", doctorId);
        return ApiResponse.error("Doctor service unavailable", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ApiResponse<Map<String, Object>> getDoctorStatistics(Long doctorId) {
        log.error("DoctorService unavailable - getDoctorStatistics fallback for doctorId: {}", doctorId);
        return ApiResponse.error("Doctor service unavailable", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ApiResponse<Map<String, Object>> canDoctorAcceptCases(Long doctorId) {
        log.error("DoctorService unavailable - canDoctorAcceptCases fallback for doctorId: {}", doctorId);
        Map<String, Object> result = new HashMap<>();
        result.put("canAcceptCases", false);
        result.put("reason", "Doctor service unavailable");
        return ApiResponse.error("Doctor service unavailable", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}