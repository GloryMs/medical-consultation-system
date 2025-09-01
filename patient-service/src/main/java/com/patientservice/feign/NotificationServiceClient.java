package com.patientservice.feign;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.NotificationDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "notification-service")
public interface NotificationServiceClient {

    @GetMapping("/api/notifications/user/{userId}")
    ResponseEntity<ApiResponse<List<NotificationDto>>> getUserNotifications(@PathVariable Long userId);
}