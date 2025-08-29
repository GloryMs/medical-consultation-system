package com.adminservice.feign;

import com.adminservice.dto.DoctorDetailsDto;
import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.DoctorDto;
import com.commonlibrary.dto.DoctorProfileDto;
import com.commonlibrary.dto.PendingVerificationDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
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
    ResponseEntity<ApiResponse<Void>> updateDoctorVerification(@PathVariable Long doctorId,
                                                               @RequestParam String status,
                                                               @RequestParam String reason);

    @GetMapping("/api/doctors/pending-verifications")
    ResponseEntity<ApiResponse<List<PendingVerificationDto>>> getPendingVerifications();

    @GetMapping("/api/doctors/pending-verifications/count")
    ResponseEntity<ApiResponse<Long>> getPendingVerificationsCount();

    @GetMapping("/api/profile/{doctorId}")
    ResponseEntity<ApiResponse<DoctorProfileDto>> getDoctorDetails(@PathVariable Long doctorId);

    @GetMapping("/api/doctors/{doctorId}/performance")
    ResponseEntity<ApiResponse<Map<String, Object>>> getDoctorPerformance(@PathVariable Long doctorId,
                                             @RequestParam LocalDate startDate,
                                             @RequestParam LocalDate endDate);
}
