package com.adminservice.feign;

import com.adminservice.dto.DoctorDetailsDto;
import com.adminservice.dto.PendingVerificationDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@FeignClient(name = "doctor-service")
public interface DoctorServiceClient {

    @PutMapping("/api/doctors/{doctorId}/verification")
    void updateDoctorVerification(@PathVariable Long doctorId,
                                  @RequestParam String status,
                                  @RequestParam String reason);

    @GetMapping("/api/doctors/pending-verifications")
    List<PendingVerificationDto> getPendingVerifications();

    @GetMapping("/api/doctors/pending-verifications/count")
    Long getPendingVerificationsCount();

    @GetMapping("/api/doctors/{doctorId}")
    DoctorDetailsDto getDoctorDetails(@PathVariable Long doctorId);

    @GetMapping("/api/doctors/{doctorId}/performance")
    Map<String, Object> getDoctorPerformance(@PathVariable Long doctorId,
                                             @RequestParam LocalDate startDate,
                                             @RequestParam LocalDate endDate);
}
