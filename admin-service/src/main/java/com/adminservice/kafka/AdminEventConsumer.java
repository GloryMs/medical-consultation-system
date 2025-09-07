package com.adminservice.kafka;

import com.adminservice.service.AdminService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminEventConsumer {

    private final AdminService adminService;

    @KafkaListener(topics = "user-registration-topic",groupId = "admin-group")
    public void handleUserRegistration(Map<String, Object> registrationEvent) {
        try {
            Long userId = Long.valueOf(registrationEvent.get("userId").toString());
            String email = registrationEvent.get("email").toString();
            String role = registrationEvent.get("role").toString();
            
            log.info("New user registered: {}, role: {}", email, role);
            
            // Send admin notification for new doctor registrations
            if ("DOCTOR".equals(role)) {
                adminService.notifyAdminOfNewDoctor(userId, email);
            }
            
        } catch (Exception e) {
            log.error("Error processing user registration event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "payment-completed-topic", groupId = "admin-group")
    public void handlePaymentCompleted(Map<String, Object> paymentEvent) {
        try {
            String paymentType = paymentEvent.get("paymentType").toString();
            Double amount = Double.valueOf(paymentEvent.get("amount").toString());
            
            log.info("Payment completed: type={}, amount={}", paymentType, amount);
            
            // Update financial statistics
            adminService.updatePaymentStats(paymentType, amount);
            
        } catch (Exception e) {
            log.error("Error processing payment event: {}", e.getMessage(), e);
        }
    }
}