package com.messagingservice.feign;

import com.commonlibrary.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

/**
 * Feign client for Supervisor Service
 * Used to fetch assigned patient IDs for medical supervisors
 */
@FeignClient(name = "supervisor-service")
public interface SupervisorServiceClient {

    /**
     * Get list of patient IDs assigned to a supervisor
     * @param supervisorId The supervisor's user ID
     * @return ApiResponse containing list of patient IDs
     */
    @GetMapping("/api/supervisors/patients/userIds")
    ResponseEntity<ApiResponse<List<Long>>> getAssignedPatientIds(
            @RequestHeader("X-User-Id") Long supervisorId
    );
}
