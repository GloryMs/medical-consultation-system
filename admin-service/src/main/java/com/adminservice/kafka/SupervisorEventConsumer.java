package com.adminservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes events from supervisor-service for admin monitoring
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SupervisorEventConsumer {
    
    /**
     * Handle supervisor registered event
     */
    @KafkaListener(topics = "supervisor.registered", groupId = "admin-service-group")
    public void handleSupervisorRegistered(Map<String, Object> event) {
        try {
            Long supervisorId = Long.valueOf(event.get("supervisorId").toString());
            String email = event.get("email").toString();
            
            log.info("New supervisor registered - ID: {}, Email: {}", supervisorId, email);
            
            // Trigger admin notification for verification
            // Update admin dashboard statistics
            
        } catch (Exception e) {
            log.error("Error handling supervisor registered event", e);
        }
    }
    
    /**
     * Handle supervisor verified event
     */
    @KafkaListener(topics = "supervisor.verified", groupId = "admin-service-group")
    public void handleSupervisorVerified(Map<String, Object> event) {
        try {
            Long supervisorId = Long.valueOf(event.get("supervisorId").toString());
            Long verifiedBy = Long.valueOf(event.get("verifiedBy").toString());
            
            log.info("Supervisor {} verified by admin {}", supervisorId, verifiedBy);
            
            // Update admin statistics
            // Log admin action
            
        } catch (Exception e) {
            log.error("Error handling supervisor verified event", e);
        }
    }
    
    /**
     * Handle supervisor suspended event
     */
    @KafkaListener(topics = "supervisor.suspended", groupId = "admin-service-group")
    public void handleSupervisorSuspended(Map<String, Object> event) {
        try {
            Long supervisorId = Long.valueOf(event.get("supervisorId").toString());
            String reason = event.get("reason").toString();
            
            log.info("Supervisor {} suspended - reason: {}", supervisorId, reason);
            
            // Update admin statistics
            // Trigger compliance review if needed
            
        } catch (Exception e) {
            log.error("Error handling supervisor suspended event", e);
        }
    }
}