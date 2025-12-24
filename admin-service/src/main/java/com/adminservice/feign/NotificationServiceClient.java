package com.adminservice.feign;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.NotificationDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "notification-service")
public interface NotificationServiceClient {
    
//    @PostMapping("/api/notifications/send")
//    void sendNotification(@RequestParam Long senderId,
//                         @RequestParam Long receiverId,
//                         @RequestParam String title,
//                         @RequestParam String message);

    @GetMapping("/api/notifications/user/{userId}")
    ResponseEntity<ApiResponse<List<NotificationDto>>> getUserNotifications(@PathVariable Long userId);

    @PutMapping("/api/notifications/{notificationId}/{userId}/read")
    ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long notificationId,
            @PathVariable Long userId);

    @PutMapping("/api/notifications/user/{userId}/read-all")
    ResponseEntity<ApiResponse<Void>> markAllAsRead( @PathVariable Long userId);
}