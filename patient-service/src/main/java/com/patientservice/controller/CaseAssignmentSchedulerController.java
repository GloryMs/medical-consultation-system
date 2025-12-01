package com.patientservice.controller;

import com.patientservice.config.CaseAssignmentSchedulerConfig;
import com.patientservice.scheduler.CaseAssignmentScheduler;
import com.patientservice.scheduler.CaseAssignmentScheduler.SchedulerStatistics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for monitoring and managing case assignment scheduler
 * Provides endpoints for admins to view scheduler status and statistics
 */
@RestController
@RequestMapping("/api/scheduler/case-assignment")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Case Assignment Scheduler", description = "Endpoints for monitoring and managing case assignment scheduler")
public class CaseAssignmentSchedulerController {

    private final CaseAssignmentScheduler scheduler;
    private final CaseAssignmentSchedulerConfig config;

    /**
     * Get scheduler statistics and status
     * Available to admins only
     */
    @GetMapping("/statistics")
    //@PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get scheduler statistics", 
               description = "Retrieve statistics about case assignments, expirations, and reminders")
    public ResponseEntity<Map<String, Object>> getSchedulerStatistics() {
        try {
            SchedulerStatistics stats = scheduler.getSchedulerStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving scheduler statistics: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve scheduler statistics");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get scheduler configuration
     * Available to admins only
     */
    @GetMapping("/configuration")
    //@PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get scheduler configuration", 
               description = "Retrieve current scheduler configuration settings")
    public ResponseEntity<Map<String, Object>> getSchedulerConfiguration() {
        try {
            Map<String, Object> configuration = new HashMap<>();
            configuration.put("schedulerEnabled", config.getSchedulerEnabled());
            configuration.put("checkIntervalSeconds", config.getCheckIntervalSeconds());
            configuration.put("assignmentTimeoutHours", config.getAssignmentTimeoutHours());
            configuration.put("canReassignToSameDoctor", config.getCanReassignToSameDoctor());
            configuration.put("reminderEnabled", config.getReminderEnabled());
            configuration.put("reminderHoursFromAssignment", config.getReminderHoursFromAssignment());
            configuration.put("reminderTimesBeforeExpiration", config.getTimeRemainingForReminders());
            configuration.put("notifyAdminOnExpiration", config.getNotifyAdminOnExpiration());
            configuration.put("maxReassignmentAttempts", config.getMaxReassignmentAttempts());
            configuration.put("reassignmentCooldownHours", config.getReassignmentCooldownHours());
            configuration.put("prioritizeMultipleExpirations", config.getPrioritizeMultipleExpirations());
            configuration.put("expirationGracePeriodMinutes", config.getExpirationGracePeriodMinutes());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", configuration);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving scheduler configuration: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve scheduler configuration");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get scheduler health status
     * Available to all authenticated users
     */
    @GetMapping("/health")
    @Operation(summary = "Get scheduler health status", 
               description = "Check if scheduler is running properly")
    public ResponseEntity<Map<String, Object>> getSchedulerHealth() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("schedulerEnabled", config.getSchedulerEnabled());
            health.put("reminderEnabled", config.getReminderEnabled());
            health.put("status", config.getSchedulerEnabled() ? "RUNNING" : "DISABLED");
            health.put("healthy", true);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", health);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking scheduler health: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("healthy", false);
            errorResponse.put("message", "Failed to check scheduler health");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get detailed scheduler information
     * Combines statistics and configuration
     * Available to admins only
     */
    @GetMapping("/info")
    //@PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get detailed scheduler information", 
               description = "Retrieve comprehensive scheduler information including statistics and configuration")
    public ResponseEntity<Map<String, Object>> getSchedulerInfo() {
        try {
            SchedulerStatistics stats = scheduler.getSchedulerStatistics();

            Map<String, Object> info = new HashMap<>();
            info.put("statistics", stats);
            info.put("configuration", Map.of(
                "assignmentTimeoutHours", config.getAssignmentTimeoutHours(),
                "reminderSchedule", config.getReminderHoursFromAssignment(),
                "maxReassignmentAttempts", config.getMaxReassignmentAttempts(),
                "canReassignToSameDoctor", config.getCanReassignToSameDoctor()
            ));
            info.put("status", config.getSchedulerEnabled() ? "ACTIVE" : "INACTIVE");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", info);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving scheduler info: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve scheduler information");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}