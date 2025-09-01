package com.doctorservice.controller;

import com.commonlibrary.dto.ApiResponse;
import com.doctorservice.dto.DoctorCapacityDto;
import com.doctorservice.dto.DoctorWorkloadDto;
import com.doctorservice.service.DoctorWorkloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

// Internal DoctorWorkloadController for service-to-service communication
@RestController
@RequestMapping("/internal/doctors/workload")
@RequiredArgsConstructor
@Slf4j
public class InternalDoctorWorkloadController {

    private final DoctorWorkloadService workloadService;

    /**
     * Internal endpoint for other services to trigger workload updates
     */
    @PostMapping("/{doctorId}/update")
    public ResponseEntity<ApiResponse<String>> updateWorkloadInternal(
            @PathVariable Long doctorId,
            @RequestHeader(value = "X-Service-Name", required = false) String serviceName) {

        log.info("Internal workload update triggered for doctor {} by service: {}", doctorId, serviceName);
        workloadService.loadDoctorWorkload(doctorId);
        return ResponseEntity.ok(ApiResponse.success("Workload updated"));
    }

    /**
     * Check availability for internal service calls
     */
    @GetMapping("/{doctorId}/check-availability")
    public ResponseEntity<ApiResponse<Boolean>> checkAvailabilityInternal(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime requestedTime) {

        boolean isAvailable = workloadService.isDoctorAvailable(doctorId, requestedTime);
        return ResponseEntity.ok(ApiResponse.success(isAvailable));
    }

    /**
     * Get doctor capacity information for case assignment algorithms
     */
    @GetMapping("/{doctorId}/capacity")
    public ResponseEntity<ApiResponse<DoctorCapacityDto>> getDoctorCapacity(@PathVariable Long doctorId) {

        DoctorWorkloadDto workload = workloadService.getDoctorWorkload(doctorId);

        // Convert to capacity DTO
        DoctorCapacityDto capacity = DoctorCapacityDto.builder()
                .doctorId(workload.getDoctorId())
                .activeCases(workload.getActiveCases())
                .maxActiveCases(workload.getMaxActiveCases())
                .todayAppointments(workload.getTodayAppointments())
                .maxDailyAppointments(workload.getMaxDailyAppointments())
                .workloadPercentage(workload.getWorkloadPercentage())
                .rating(workload.getAverageRating())
                .consultationCount(workload.getConsultationCount())
                .isAvailable(workload.getIsAvailable())
                .emergencyMode(workload.getEmergencyMode())
                .build();

        return ResponseEntity.ok(ApiResponse.success(capacity));
    }

    /**
     * Get multiple doctors' capacity for batch operations
     */
    @PostMapping("/batch-capacity")
    public ResponseEntity<ApiResponse<List<DoctorCapacityDto>>> getBatchCapacity(
            @RequestBody List<Long> doctorIds) {

        List<DoctorCapacityDto> capacities = doctorIds.stream()
                .map(this::buildCapacityDto)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(capacities));
    }

    private DoctorCapacityDto buildCapacityDto(Long doctorId) {
        try {
            DoctorWorkloadDto workload = workloadService.getDoctorWorkload(doctorId);
            return DoctorCapacityDto.builder()
                    .doctorId(workload.getDoctorId())
                    .activeCases(workload.getActiveCases())
                    .maxActiveCases(workload.getMaxActiveCases())
                    .todayAppointments(workload.getTodayAppointments())
                    .maxDailyAppointments(workload.getMaxDailyAppointments())
                    .workloadPercentage(workload.getWorkloadPercentage())
                    .rating(workload.getAverageRating())
                    .consultationCount(workload.getConsultationCount())
                    .isAvailable(workload.getIsAvailable())
                    .emergencyMode(workload.getEmergencyMode())
                    .build();
        } catch (Exception e) {
            log.error("Error getting capacity for doctor {}: {}", doctorId, e.getMessage());
            return DoctorCapacityDto.builder()
                    .doctorId(doctorId)
                    .isAvailable(false)
                    .workloadPercentage(100.0)
                    .build();
        }
    }
}
