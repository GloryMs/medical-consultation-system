package com.patientservice.feign;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.NotificationDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "notification-service")
public interface NotificationServiceClient {

    @GetMapping("/api/notifications/user/{userId}")
    ResponseEntity<ApiResponse<List<NotificationDto>>> getUserNotifications(@PathVariable Long userId);

    @PutMapping("/api/notifications/{notificationId}/{userId}/read")
    ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long notificationId,
            @PathVariable Long userId);

    @PutMapping("/api/notifications/user/{userId}/read-all")
    ResponseEntity<ApiResponse<Void>> markAllAsRead( @PathVariable Long userId);

    /**
     * Send welcome email to newly created patient
     * @param email Patient's email address
     * @param fullName Patient's full name
     * @param temporaryPassword Temporary password for first login
     */
    @PostMapping("/api/notifications/welcome-email")
    ResponseEntity<ApiResponse<Void>> sendWelcomeEmail(
            @RequestParam("email") String email,
            @RequestParam("fullName") String fullName,
            @RequestParam("temporaryPassword") String temporaryPassword);
}