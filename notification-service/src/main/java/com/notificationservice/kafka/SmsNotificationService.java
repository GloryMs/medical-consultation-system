package com.notificationservice.kafka;

import com.commonlibrary.dto.SmsNotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsNotificationService {

    // TODO: Integrate with SMS provider (Twilio, AWS SNS, etc.)
    
    @KafkaListener(topics = "sms-notifications", groupId = "notification-group")
    public void consumeSmsNotification(SmsNotificationDto notification) {
        try {
            log.info("Received SMS notification for: {}", notification.getPhoneNumber());
            
            // Send SMS based on provider
            if ("WHATSAPP".equalsIgnoreCase(notification.getProvider())) {
                sendWhatsAppMessage(notification);
            } else {
                sendSmsMessage(notification);
            }
            
            log.info("SMS sent successfully to: {}", notification.getPhoneNumber());
        } catch (Exception e) {
            log.error("Error sending SMS to: {}", notification.getPhoneNumber(), e);
            // TODO: Implement retry logic or dead letter queue
        }
    }
    
    private void sendSmsMessage(SmsNotificationDto notification) {
        // TODO: Implement SMS sending logic using Twilio or similar service
        log.info("Sending SMS to: {} with message: {}", 
                notification.getPhoneNumber(), notification.getMessage());
        
        // Example Twilio integration (uncomment when ready):
        /*
        Twilio.init(accountSid, authToken);
        Message message = Message.creator(
                new PhoneNumber(notification.getPhoneNumber()),
                new PhoneNumber(twilioPhoneNumber),
                notification.getMessage()
        ).create();
        */
    }
    
    private void sendWhatsAppMessage(SmsNotificationDto notification) {
        // TODO: Implement WhatsApp sending logic using WhatsApp Business API
        log.info("Sending WhatsApp message to: {} with message: {}", 
                notification.getPhoneNumber(), notification.getMessage());
        
        // Example WhatsApp integration (uncomment when ready):
        /*
        Twilio.init(accountSid, authToken);
        Message message = Message.creator(
                new PhoneNumber("whatsapp:" + notification.getPhoneNumber()),
                new PhoneNumber("whatsapp:" + twilioWhatsAppNumber),
                notification.getMessage()
        ).create();
        */
    }
}