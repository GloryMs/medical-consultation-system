package com.supervisorservice.controller;

import com.supervisorservice.dto.ApiResponse;
import com.commonlibrary.dto.SupervisorSettingsDto;
import com.supervisorservice.entity.SupervisorSettings;
import com.supervisorservice.exception.ResourceNotFoundException;
import com.supervisorservice.repository.SupervisorSettingsRepository;
import com.supervisorservice.service.SupervisorValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for supervisor settings operations
 */
@RestController
@RequestMapping("/api/supervisors/settings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Supervisor Settings", description = "Supervisor notification and preference settings")
public class SupervisorSettingsController {
    
    private final SupervisorSettingsRepository settingsRepository;
    private final SupervisorValidationService validationService;
    
    /**
     * Get supervisor settings
     */
    @GetMapping
    @Operation(summary = "Get settings", 
               description = "Retrieves supervisor notification and preference settings")
    public ResponseEntity<ApiResponse<SupervisorSettingsDto>> getSettings(
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("GET /api/supervisors/settings - userId: {}", userId);
        
        validationService.validateSupervisorActive(userId);
        
        SupervisorSettings settings = settingsRepository.findBySupervisorUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Settings not found"));
        
        return ResponseEntity.ok(ApiResponse.success(mapToDto(settings)));
    }
    
    /**
     * Update supervisor settings
     */
    @PutMapping
    @Operation(summary = "Update settings", 
               description = "Updates supervisor notification and preference settings")
    public ResponseEntity<ApiResponse<SupervisorSettingsDto>> updateSettings(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody SupervisorSettingsDto request) {
        
        log.info("PUT /api/supervisors/settings - userId: {}", userId);
        
        validationService.validateSupervisorActive(userId);
        
        SupervisorSettings settings = settingsRepository.findBySupervisorUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Settings not found"));
        
        // Update settings
        if (request.getEmailNotifications() != null) {
            settings.setEmailNotifications(request.getEmailNotifications());
        }
        if (request.getSmsNotifications() != null) {
            settings.setSmsNotifications(request.getSmsNotifications());
        }
        if (request.getPushNotifications() != null) {
            settings.setPushNotifications(request.getPushNotifications());
        }
        if (request.getNewCaseAssignmentNotification() != null) {
            settings.setNewCaseAssignmentNotification(request.getNewCaseAssignmentNotification());
        }
        if (request.getAppointmentRemindersNotification() != null) {
            settings.setAppointmentRemindersNotification(request.getAppointmentRemindersNotification());
        }
        if (request.getCaseStatusUpdateNotification() != null) {
            settings.setCaseStatusUpdateNotification(request.getCaseStatusUpdateNotification());
        }
        if (request.getCouponIssuedNotification() != null) {
            settings.setCouponIssuedNotification(request.getCouponIssuedNotification());
        }
        if (request.getCouponExpiringNotification() != null) {
            settings.setCouponExpiringNotification(request.getCouponExpiringNotification());
        }
        if (request.getPreferredLanguage() != null) {
            settings.setPreferredLanguage(request.getPreferredLanguage());
        }
        if (request.getTimezone() != null) {
            settings.setTimezone(request.getTimezone());
        }
        if (request.getTheme() != null) {
            settings.setTheme(request.getTheme());
        }
        
        settings = settingsRepository.save(settings);
        
        return ResponseEntity.ok(ApiResponse.success("Settings updated successfully", mapToDto(settings)));
    }
    
    /**
     * Map entity to DTO
     */
    private SupervisorSettingsDto mapToDto(SupervisorSettings settings) {
        return SupervisorSettingsDto.builder()
                .id(settings.getId())
                .supervisorId(settings.getSupervisor().getId())
                .emailNotifications(settings.getEmailNotifications())
                .smsNotifications(settings.getSmsNotifications())
                .pushNotifications(settings.getPushNotifications())
                .newCaseAssignmentNotification(settings.getNewCaseAssignmentNotification())
                .appointmentRemindersNotification(settings.getAppointmentRemindersNotification())
                .caseStatusUpdateNotification(settings.getCaseStatusUpdateNotification())
                .couponIssuedNotification(settings.getCouponIssuedNotification())
                .couponExpiringNotification(settings.getCouponExpiringNotification())
                .preferredLanguage(settings.getPreferredLanguage())
                .timezone(settings.getTimezone())
                .theme(settings.getTheme())
                .build();
    }
}
