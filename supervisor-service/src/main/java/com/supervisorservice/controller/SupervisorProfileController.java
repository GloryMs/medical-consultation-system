package com.supervisorservice.controller;

import com.commonlibrary.dto.NotificationDto;
import com.supervisorservice.dto.ApiResponse;
import com.supervisorservice.dto.CreateSupervisorProfileRequest;
import com.commonlibrary.dto.SupervisorProfileDto;
import com.supervisorservice.dto.UpdateSupervisorProfileRequest;
import com.supervisorservice.service.SupervisorNotificationService;
import com.supervisorservice.service.SupervisorProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * REST controller for supervisor profile operations
 */
@RestController
@RequestMapping("/api/supervisors")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Supervisor Profile", description = "Supervisor profile management endpoints")
public class SupervisorProfileController {
    
    private final SupervisorProfileService profileService;
    private final SupervisorNotificationService notificationService;
    
    /**
     * Create supervisor profile
     */
    @PostMapping("/profile")
    @Operation(summary = "Create supervisor profile", 
               description = "Creates a new supervisor profile with PENDING verification status")
    public ResponseEntity<ApiResponse<SupervisorProfileDto>> createProfile(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateSupervisorProfileRequest request) {
        
        log.info("POST /api/supervisors/profile - userId: {}", userId);
        
        SupervisorProfileDto profile = profileService.createProfile(userId, request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Profile created successfully. Awaiting admin verification.", profile));
    }
    
    /**
     * Get supervisor profile
     */
    @GetMapping("/profile")
    @Operation(summary = "Get supervisor profile", 
               description = "Retrieves the authenticated supervisor's profile")
    public ResponseEntity<ApiResponse<SupervisorProfileDto>> getProfile(
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("GET /api/supervisors/profile - userId: {}", userId);
        
        SupervisorProfileDto profile = profileService.getProfile(userId);
        
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
    
    /**
     * Update supervisor profile
     */
    @PutMapping("/profile")
    @Operation(summary = "Update supervisor profile", 
               description = "Updates supervisor profile information")
    public ResponseEntity<ApiResponse<SupervisorProfileDto>> updateProfile(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UpdateSupervisorProfileRequest request) {
        
        log.info("PUT /api/supervisors/profile - userId: {}", userId);
        
        SupervisorProfileDto profile = profileService.updateProfile(userId, request);
        
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", profile));
    }
    
    /**
     * Upload license document
     */
    @PostMapping(value = "/profile/license-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload license document", 
               description = "Uploads professional license document for verification")
    public ResponseEntity<ApiResponse<String>> uploadLicenseDocument(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("file") MultipartFile file) throws IOException {
        
        log.info("POST /api/supervisors/profile/license-document - userId: {}, filename: {}", 
                userId, file.getOriginalFilename());
        
        String filePath = profileService.uploadLicenseDocument(userId, file);
        
        return ResponseEntity.ok(ApiResponse.success("License document uploaded successfully", filePath));
    }
    
    /**
     * Delete supervisor profile
     */
    @DeleteMapping("/profile")
    @Operation(summary = "Delete supervisor profile", 
               description = "Soft deletes the supervisor profile")
    public ResponseEntity<ApiResponse<Void>> deleteProfile(
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("DELETE /api/supervisors/profile - userId: {}", userId);
        
        profileService.deleteProfile(userId);
        
        return ResponseEntity.ok(ApiResponse.success("Profile deleted successfully", null));
    }

    //========== Notifications ==========


    @GetMapping ("/notifications/{userId}")
    public ResponseEntity<com.commonlibrary.dto.ApiResponse<List<NotificationDto>>> getNotification(
            @PathVariable Long userId){
        List<NotificationDto> notificationsDto = new ArrayList<>();
        notificationsDto = notificationService.getMyNotifications(userId);
        return ResponseEntity.ok(com.commonlibrary.dto.ApiResponse.success(notificationsDto));
    }

    @PutMapping("/notifications/{notificationId}/{userId}/read")
    public ResponseEntity<com.commonlibrary.dto.ApiResponse<Void>> markAsRead(
            @PathVariable Long notificationId,
            @PathVariable Long userId){
        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok(com.commonlibrary.dto.ApiResponse.success(null, "Marked as read"));
    }

    @PutMapping("/notifications/{userId}/read-all")
    public ResponseEntity<com.commonlibrary.dto.ApiResponse<Void>> markAllAsRead(
            @PathVariable  Long userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(com.commonlibrary.dto.ApiResponse.success(null, "All notifications marked as read"));
    }

}
