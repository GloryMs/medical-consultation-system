package com.patientservice.feign;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.NotificationDto;
import com.commonlibrary.entity.UserType;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "notification-service")
public interface NotificationServiceClient {

//    @GetMapping("/api/notifications/user/{userId}")
//    ResponseEntity<ApiResponse<List<NotificationDto>>> getUserNotifications(@PathVariable Long userId);
//
//    @PutMapping("/api/notifications/{notificationId}/{userId}/read")
//    ResponseEntity<ApiResponse<Void>> markAsRead(
//            @PathVariable Long notificationId,
//            @PathVariable Long userId);
//
//    @PutMapping("/api/notifications/user/{userId}/read-all")
//    ResponseEntity<ApiResponse<Void>> markAllAsRead( @PathVariable Long userId);

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



    /**
     * Get all notifications for a user
     */
    @GetMapping("/api/notifications/user/{userId}")
    ApiResponse<List<NotificationDto>> getUserNotifications(
            @PathVariable("userId") Long userId,
            @RequestParam("userType") UserType userType
    );

    /**
     * Get unread notifications for a user
     */
    @GetMapping("/api/notifications/user/{userId}/unread")
    ApiResponse<List<NotificationDto>> getUnreadNotifications(
            @PathVariable("userId") Long userId,
            @RequestParam("userType") UserType userType
    );

    /**
     * Mark a notification as read
     */
    @PutMapping("/api/notifications/{notificationId}/read")
    ApiResponse<Void> markAsRead(
            @PathVariable("notificationId") Long notificationId,
            @RequestParam("userId") Long userId,
            @RequestParam("userType") UserType userType
    );

    /**
     * Mark all notifications as read
     */
    @PutMapping("/api/notifications/user/{userId}/read-all")
    ApiResponse<Void> markAllAsRead(
            @PathVariable("userId") Long userId,
            @RequestParam("userType") UserType userType
    );

    /**
     * Get unread notification count
     */
    @GetMapping("/api/notifications/user/{userId}/count")
    ApiResponse<Long> getUnreadNotificationCount(
            @PathVariable("userId") Long userId,
            @RequestParam("userType") UserType userType
    );
}