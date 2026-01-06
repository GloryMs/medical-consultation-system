package com.supervisorservice.controller;

import com.supervisorservice.dto.ApiResponse;
import com.supervisorservice.dto.RecentActivityDto;
import com.supervisorservice.dto.SupervisorDashboardDto;
import com.supervisorservice.service.SupervisorDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for supervisor dashboard operations
 */
@RestController
@RequestMapping("/api/supervisors/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Supervisor Dashboard", description = "Dashboard statistics and recent activity")
public class SupervisorDashboardController {
    
    private final SupervisorDashboardService dashboardService;
    
    /**
     * Get dashboard statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get dashboard statistics", 
               description = "Retrieves comprehensive dashboard statistics for supervisor")
    public ResponseEntity<ApiResponse<SupervisorDashboardDto>> getDashboardStatistics(
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("GET /api/supervisors/dashboard/statistics - userId: {}", userId);
        
        SupervisorDashboardDto dashboard = dashboardService.getDashboardStatistics(userId);
        
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }
    
    /**
     * Get recent activity
     */
    @GetMapping("/activity")
    @Operation(summary = "Get recent activity", 
               description = "Retrieves recent activity timeline for supervisor")
    public ResponseEntity<ApiResponse<List<RecentActivityDto>>> getRecentActivity(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "20") Integer limit) {
        
        log.info("GET /api/supervisors/dashboard/activity - userId: {}, limit: {}", userId, limit);
        
        List<RecentActivityDto> activities = dashboardService.getRecentActivity(userId, limit);
        
        return ResponseEntity.ok(ApiResponse.success(activities));
    }
    
    /**
     * Get performance metrics
     */
    @GetMapping("/metrics")
    @Operation(summary = "Get performance metrics", 
               description = "Retrieves performance metrics and analytics")
    public ResponseEntity<ApiResponse<Object>> getPerformanceMetrics(
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("GET /api/supervisors/dashboard/metrics - userId: {}", userId);
        
        Object metrics = dashboardService.getPerformanceMetrics(userId);
        
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }
}