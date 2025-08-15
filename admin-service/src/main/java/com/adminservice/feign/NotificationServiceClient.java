package com.adminservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "notification-service")
public interface NotificationServiceClient {
    
    @PostMapping("/api/notifications/send")
    void sendNotification(@RequestParam Long senderId,
                         @RequestParam Long receiverId,
                         @RequestParam String title,
                         @RequestParam String message);
}