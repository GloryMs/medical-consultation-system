package com.adminservice.feign;

import com.commonlibrary.dto.*;
import com.commonlibrary.entity.SupervisorVerificationStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "supervisor-service")
public interface SupervisorServiceClient {

    @GetMapping("/api/admin/supervisors")
    ResponseEntity<ApiResponse<List<SupervisorProfileDto>>> getAllSupervisors(
            @RequestParam(required = false) SupervisorVerificationStatus status);

    @GetMapping("/api/admin/supervisors/pending")
    ResponseEntity<ApiResponse<List<SupervisorProfileDto>>> getPendingSupervisors();

    @GetMapping("/api/admin/supervisors/{supervisorId}")
    ResponseEntity<ApiResponse<SupervisorProfileDto>> getSupervisor(@PathVariable Long supervisorId);

    @PutMapping("/api/admin/supervisors/{supervisorId}/verify")
    ResponseEntity<ApiResponse<SupervisorProfileDto>> verifySupervisor(
            @RequestHeader("X-User-Id") Long adminUserId,
            @PathVariable Long supervisorId,
            @Valid @RequestBody VerifySupervisorRequest request);

    @PutMapping("/api/admin/supervisors/{supervisorId}/reject")
    ResponseEntity<ApiResponse<SupervisorProfileDto>> rejectSupervisor(
            @PathVariable Long supervisorId,
            @Valid @RequestBody RejectSupervisorRequest request);

    @PutMapping("/api/admin/supervisors/{supervisorId}/suspend")
    ResponseEntity<ApiResponse<SupervisorProfileDto>> suspendSupervisor(
            @PathVariable Long supervisorId,
            @RequestParam String reason);

    @PutMapping("/api/admin/supervisors/{supervisorId}/limits")
    ResponseEntity<ApiResponse<SupervisorProfileDto>> updateLimits(
            @PathVariable Long supervisorId,
            @Valid @RequestBody UpdateSupervisorLimitsRequest request);

    @GetMapping("/api/admin/supervisors/search")
    ResponseEntity<ApiResponse<List<SupervisorProfileDto>>> searchSupervisors(
            @RequestParam String query);

    @GetMapping("/api/admin/supervisors/statistics")
    ResponseEntity<ApiResponse<SupervisorStatisticsDto>> getSupervisorStatistics();
}
