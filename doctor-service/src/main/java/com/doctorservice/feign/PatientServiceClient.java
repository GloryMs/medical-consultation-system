package com.doctorservice.feign;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.CustomPatientDto;
import com.commonlibrary.dto.RescheduleRequestResponseDto;
import com.doctorservice.dto.CaseAssignmentDto;
import com.commonlibrary.dto.CaseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@FeignClient(name = "patient-service")
public interface PatientServiceClient {

    @PutMapping("/api/patients/cases/{caseId}/status")
    void updateCaseStatus(@PathVariable Long caseId,
                          @RequestParam String status,
                          @RequestParam Long doctorId);

    @GetMapping("/api/patients/cases/doctor/{doctorId}")
    ResponseEntity<ApiResponse<List<CaseDto>>> getNewAssignedCasesForDoctor(@PathVariable Long doctorId);

    @GetMapping("/api/patients/cases/pool")
    ResponseEntity<ApiResponse<List<CaseDto>>> getCasesPool(@RequestParam String specialization);

//    @PutMapping("/api/patients/cases/{caseId}/reject")
//    void rejectCase(@PathVariable Long caseId,
//                    @RequestParam Long doctorId,
//                    @RequestParam String reason);
//
//    @PutMapping("/api/patients/cases/{caseId}/fee")
//    void setCaseFee(@PathVariable Long caseId,
//                    @RequestParam BigDecimal fee,
//                    @RequestParam String reason);

    @PutMapping("/api/patients/cases/{caseId}/close")
    ResponseEntity<ApiResponse<Void>> closeCase(@PathVariable Long caseId,
                   @RequestParam Long doctorId,
                   @RequestParam String notes);

    @PostMapping("/api/patients/cases/{caseId}/claim")
    ResponseEntity<ApiResponse<Void>> claimCase(@PathVariable Long caseId,
                                                @RequestParam Long doctorId,
                                                @RequestParam String notes);

    @PostMapping("/api/patients/case-assignment/{doctorId}/assignment/{assignmentId}")
    ResponseEntity<ApiResponse<Void>> acceptAssignment(@PathVariable Long doctorId,
                   @RequestParam Long assignmentId);

    @PostMapping("/api/patients/case-assignment/{doctorId}/assignment/{assignmentId}/reject")
    ResponseEntity<ApiResponse<Void>> rejectAssignment(@PathVariable Long doctorId,
                          @RequestParam Long assignmentId,
                          @RequestParam String reason);

    @GetMapping("/api/patients/case-assignments")
    ResponseEntity<ApiResponse<List<CaseAssignmentDto>>> findByDoctorIdAndStatus(@RequestParam Long doctorId,
                                                                                 @RequestParam String status);


    @GetMapping("/api/patients/cases/doctor/{doctorId}/active")
    ResponseEntity<ApiResponse<List<CaseDto>>> getDoctorActiveCases(@PathVariable("doctorId") Long doctorId);


    @GetMapping("/api/patients/cases/doctor/{doctorId}/all")
    ResponseEntity<ApiResponse<List<CaseDto>>> getAllDoctorCases(@PathVariable("doctorId") Long doctorId);

    @GetMapping("/api/patients/cases/doctor/{doctorId}/completed")
    ResponseEntity<ApiResponse<List<CaseDto>>> getDoctorCompletedCases(@PathVariable("doctorId") Long doctorId);

    @GetMapping("/api/patients/cases/doctor/{doctorId}/closed")
    ResponseEntity<ApiResponse<List<CaseDto>>> getDoctorClosedCases(@PathVariable("doctorId") Long doctorId);

//    @PostMapping("/cases/{caseId}/reject")
//    ResponseEntity<ApiResponse<Void>> rejectCase(
//            @PathVariable("caseId") Long caseId,
//            @RequestParam("doctorId") Long doctorId,
//            @RequestParam("reason") String reason
//    );

    @PostMapping("/cases/{caseId}/fee")
    ResponseEntity<ApiResponse<Void>> setCaseFee(
            @PathVariable("caseId") Long caseId,
            @RequestParam("consultationFee") java.math.BigDecimal consultationFee,
            @RequestParam("reason") String reason
    );

    // New workload-specific endpoints
    @GetMapping("/cases/doctor/{doctorId}/count/active")
    ResponseEntity<ApiResponse<Integer>> getActiveCasesCount(@PathVariable("doctorId") Long doctorId);

    @GetMapping("/cases/doctor/{doctorId}/metrics")
    ResponseEntity<ApiResponse<Object>> getDoctorCaseMetrics(@PathVariable("doctorId") Long doctorId);

    @GetMapping("/api/patients/cases/{caseId}/custom-info")
    ResponseEntity<ApiResponse<CustomPatientDto>> getCustomPatientInfo( @PathVariable Long caseId,
                                                                        @RequestParam Long doctorId );

    @GetMapping("/api/patients/reschedule-requests/{requestId}")
    Optional<RescheduleRequestResponseDto> getRescheduleRequest(
            @PathVariable Long requestId
    );

    @PutMapping("/api/patients/reschedule-request/{requestId}/update")
    void updateRescheduleRequestStatus(
            @PathVariable Long requestId,
            @RequestParam String status
    );
}
