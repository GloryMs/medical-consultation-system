package com.doctorservice.feign;

import com.commonlibrary.dto.ApiResponse;
import com.doctorservice.dto.CaseAssignmentDto;
import com.doctorservice.dto.CaseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "patient-service")
public interface PatientServiceClient {

    @PutMapping("/api/patients/cases/{caseId}/status")
    void updateCaseStatus(@PathVariable Long caseId,
                          @RequestParam String status,
                          @RequestParam Long doctorId);

    @GetMapping("/api/patients/cases/doctor/{doctorId}")
    ResponseEntity<ApiResponse<List<CaseDto>>> getCasesByDoctorId(@PathVariable Long doctorId);

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
}
