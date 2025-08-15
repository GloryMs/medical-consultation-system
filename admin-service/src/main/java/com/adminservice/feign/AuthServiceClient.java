package com.adminservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {
    
    @PutMapping("/api/auth/users/{userId}/status")
    void updateUserStatus(@PathVariable Long userId, 
                         @RequestParam String status,
                         @RequestParam String reason);
    
    @PostMapping("/api/auth/users/{userId}/reset-password")
    void resetPassword(@PathVariable Long userId, 
                      @RequestParam String temporaryPassword);
}