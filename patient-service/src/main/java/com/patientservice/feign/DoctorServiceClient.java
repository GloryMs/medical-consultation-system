package com.patientservice.feign;


import com.commonlibrary.dto.*;
import com.commonlibrary.entity.CaseStatus;
import com.commonlibrary.entity.VerificationStatus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "doctor-service")
public interface DoctorServiceClient {

    @GetMapping("/api/doctors/status/{status}/available/{isAvailable}")
    ResponseEntity<ApiResponse<List<DoctorDto>>> findByVerificationStatusAndIsAvailableTrue(
            @PathVariable VerificationStatus status, @PathVariable Boolean isAvailable);

    @GetMapping("/api/doctors/doctor/custom-info/{doctorId}")
    public ResponseEntity<ApiResponse<CustomDoctorInfoDto>> getDoctorCustomInfo(
            @PathVariable Long doctorId);

    @PutMapping("/api/doctors/{doctorId}/update-load/{caseStatus}")
    ResponseEntity<ApiResponse<Void>> updateDoctorLoad(@PathVariable Long doctorId,
                                                       @PathVariable CaseStatus caseStatus,
                                                       @RequestParam int flag); // 1 -> increase | 0 -> decrease

    @GetMapping("/api/doctors/appointments/{patientId}")
    ResponseEntity<ApiResponse<List<AppointmentDto>>> getPatientAppointments(@PathVariable Long patientId);

    @GetMapping("/api/doctors/upcoming-appointments/{patientId}")
    ResponseEntity<ApiResponse<List<AppointmentDto>>> getPatientUpcomingAppointments(@PathVariable Long patientId);

    // ===== NEW WORKLOAD-RELATED METHODS =====

    /**
     * Get doctors by specialization with their workload capacity information
     */
    @GetMapping("/api/doctors-internal/specialization/{specialization}/with-capacity")
    List<DoctorCapacityDto> getAvailableDoctorsBySpecializationWithCapacity(
            @PathVariable("specialization") String specialization,
            @RequestParam(defaultValue = "20") int limit
    );

    /**
     * Get emergency-available doctors (can override workload limits)
     */
    @GetMapping("/api/doctors/emergency-available")
    ResponseEntity<ApiResponse<List<DoctorCapacityDto>>> getEmergencyAvailableDoctors(
            @RequestParam(required = false) String specialization,
            @RequestParam(defaultValue = "10") int limit
    );

    /**
     * Get doctor's current workload and capacity information
     */
    @GetMapping("/api/doctors/workload/{doctorId}/capacity")
    ResponseEntity<ApiResponse<DoctorCapacityDto>> getDoctorCapacity(
            @PathVariable("doctorId") Long doctorId
    );

    /**
     * Get multiple doctors' capacity information for batch operations
     */
    @PostMapping("/api/doctors/workload/batch-capacity")
    ResponseEntity<ApiResponse<List<DoctorCapacityDto>>> getBatchDoctorCapacity(
            @RequestBody List<Long> doctorIds
    );

    /**
     * Update doctor workload (trigger recalculation)
     */
    @PostMapping("/api/internal/doctors/workload/{doctorId}/update")
    ResponseEntity<ApiResponse<String>> updateDoctorWorkload(
            @PathVariable("doctorId") Long doctorId
    );

    /**
     * Check if doctor is available at specific time
     */
    @GetMapping("/api/doctors/workload/{doctorId}/availability")
    ResponseEntity<ApiResponse<Map<String, Object>>> checkDoctorAvailability(
            @PathVariable("doctorId") Long doctorId,
            @RequestParam("requestedTime") String requestedTime
    );

    /**
     * Enable emergency mode for a doctor
     */
    @PostMapping("/api/doctors/workload/{doctorId}/emergency-mode")
    ResponseEntity<ApiResponse<String>> enableEmergencyMode(
            @PathVariable("doctorId") Long doctorId,
            @RequestBody Map<String, String> request
    );

    /**
     * Disable emergency mode for a doctor
     */
    @DeleteMapping("/workload/{doctorId}/emergency-mode")
    ResponseEntity<ApiResponse<String>> disableEmergencyMode(
            @PathVariable("doctorId") Long doctorId
    );

    /**
     * Get system-wide workload statistics
     */
    @GetMapping("/workload/statistics")
    ResponseEntity<ApiResponse<Map<String, Object>>> getSystemWorkloadStatistics();

    /**
     * Get available doctors for case assignment (workload-optimized)
     */
    @GetMapping("/workload/available-for-assignment")
    ResponseEntity<ApiResponse<List<DoctorCapacityDto>>> getAvailableDoctorsForAssignment(
            @RequestParam(required = false) String specialization,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String urgencyLevel
    );

    /**
     * Get doctor workload trends for analytics
     */
    @GetMapping("/workload/{doctorId}/trends")
    ResponseEntity<ApiResponse<Map<String, Object>>> getDoctorWorkloadTrends(
            @PathVariable("doctorId") Long doctorId,
            @RequestParam(defaultValue = "7") int days
    );

    /**
     * Validate doctor capacity for new assignment
     */
    @PostMapping("/workload/{doctorId}/validate-capacity")
    ResponseEntity<ApiResponse<Boolean>> validateDoctorCapacityForAssignment(
            @PathVariable("doctorId") Long doctorId,
            @RequestBody Map<String, Object> caseInfo
    );

    // ===== INTERNAL SERVICE ENDPOINTS =====

    /**
     * Internal endpoint for case status updates that affect workload
     */
    @PostMapping("/internal/workload/{doctorId}/case-status-update")
    ResponseEntity<ApiResponse<String>> updateWorkloadForCaseStatusChange(
            @PathVariable("doctorId") Long doctorId,
            @RequestParam("caseId") Long caseId,
            @RequestParam("oldStatus") String oldStatus,
            @RequestParam("newStatus") String newStatus
    );

    /**
     * Internal endpoint for appointment scheduling that affects workload
     */
    @PostMapping("/internal/workload/{doctorId}/appointment-scheduled")
    ResponseEntity<ApiResponse<String>> updateWorkloadForAppointmentScheduled(
            @PathVariable("doctorId") Long doctorId,
            @RequestParam("appointmentId") Long appointmentId,
            @RequestParam("scheduledTime") String scheduledTime
    );

    /**
     * Internal endpoint for appointment completion that affects workload
     */
    @PostMapping("/internal/workload/{doctorId}/appointment-completed")
    ResponseEntity<ApiResponse<String>> updateWorkloadForAppointmentCompleted(
            @PathVariable("doctorId") Long doctorId,
            @RequestParam("appointmentId") Long appointmentId
    );

    /**
     * Get doctor recommendations based on case requirements and workload
     */
    @PostMapping("/recommendations")
    ResponseEntity<ApiResponse<List<DoctorCapacityDto>>> getDoctorRecommendations(
            @RequestBody Map<String, Object> caseRequirements
    );

    /**
     * Get doctor availability forecast for next N days
     */
    @GetMapping("/workload/{doctorId}/availability-forecast")
    ResponseEntity<ApiResponse<Map<String, Object>>> getDoctorAvailabilityForecast(
            @PathVariable("doctorId") Long doctorId,
            @RequestParam(defaultValue = "7") int days
    );

    // ===== FALLBACK METHODS FOR LEGACY COMPATIBILITY =====

    /**
     * Legacy method - now triggers workload update
     * @deprecated Use updateDoctorWorkload instead
     */
    @Deprecated
    @PostMapping("/internal/{doctorId}/update-load")
    default ResponseEntity<ApiResponse<String>> updateDoctorLoad(
            @PathVariable("doctorId") Long doctorId,
            @RequestParam("status") Object status,
            @RequestParam("increment") Integer increment) {
        // Delegate to new workload update method
        return updateDoctorWorkload(doctorId);
    }

    @PutMapping("/api/doctors/appointments/confirm")
    ResponseEntity<ApiResponse<Void>> confirmAppointment(
            @RequestParam Long caseId, @RequestParam Long patientId, @RequestParam Long doctorId);

}
