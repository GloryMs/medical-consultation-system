package com.integrationservice.kafka;

import com.commonlibrary.entity.NotificationType;
import com.integrationservice.service.SmsService;
import com.integrationservice.service.WhatsAppIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class IntegrationEventConsumer {

    private final SmsService smsService;
    private final WhatsAppIntegrationService whatsAppService;

    @KafkaListener(topics = "notification-topic", groupId = "integration-group")
    public void handleNotificationForSms(Map<String, Object> notificationEvent) {
        try {
            NotificationType type =  NotificationType.valueOf(notificationEvent.get("type").toString());
            String message = notificationEvent.get("message").toString();
            String recipientPhone = (String) notificationEvent.get("recipientPhone");
            
            // Send SMS for urgent notifications
            if ("HIGH".equals(type.name()) && recipientPhone != null) {
                smsService.sendSms(recipientPhone, message);
                log.info("Urgent SMS sent to: {}", recipientPhone);
            }
            
        } catch (Exception e) {
            log.error("Error processing notification for SMS: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "case-status-updated-topic", groupId = "integration-group")
    public void handleCaseStatusForIntegration(Map<String, Object> caseEvent) {
        try {
            Long caseId = Long.valueOf(caseEvent.get("caseId").toString());
            String newStatus = caseEvent.get("newStatus").toString();
            
            // Trigger external integrations based on case status
            if ("COMPLETED".equals(newStatus)) {
                // Could trigger billing system, reporting system, etc.
                log.info("Case {} completed - triggering external integrations", caseId);
            }
            
        } catch (Exception e) {
            log.error("Error processing case status for integrations: {}", e.getMessage(), e);
        }
    }
}