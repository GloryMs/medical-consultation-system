package com.doctorservice.feign;

import com.doctorservice.dto.CaseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "patient-service")
public interface PatientServiceClient {

    @PutMapping("/api/patients/cases/{caseId}/status")
    void updateCaseStatus(@PathVariable Long caseId,
                          @RequestParam String status,
                          @RequestParam Long doctorId);

    @GetMapping("/api/patients/cases/doctor/{doctorId}")
    List<CaseDto> getCasesByDoctorId(@PathVariable Long doctorId);

    @GetMapping("/api/patients/cases/pool")
    List<CaseDto> getCasesPool(@RequestParam String specialization);

    @PutMapping("/api/patients/cases/{caseId}/reject")
    void rejectCase(@PathVariable Long caseId,
                    @RequestParam Long doctorId,
                    @RequestParam String reason);

    @PutMapping("/api/patients/cases/{caseId}/fee")
    void setCaseFee(@PathVariable Long caseId,
                    @RequestParam BigDecimal fee,
                    @RequestParam String reason);

    @PutMapping("/api/patients/cases/{caseId}/close")
    void closeCase(@PathVariable Long caseId,
                   @RequestParam Long doctorId,
                   @RequestParam String notes);
}
