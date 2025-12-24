package com.authservice.kafka;

import com.commonlibrary.dto.EmailNotificationDto;
import com.commonlibrary.dto.NotificationDto;
import com.commonlibrary.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendUserRegistrationEvent(Long userId, String email, String phoneNumber,
                                          String role, String fullName) {
        try{
            // Send user registration event
            Map<String, Object> registrationEvent = new HashMap<>();
            registrationEvent.put("userId", userId);
            registrationEvent.put("email", email);
            registrationEvent.put("phoneNumber", phoneNumber);
            registrationEvent.put("role", role);
            registrationEvent.put("fullName", fullName);
            registrationEvent.put("timestamp", System.currentTimeMillis());

            kafkaTemplate.send("user-registration-topic", registrationEvent);
            log.info("User registration event sent for: {}", email);
        }catch(Exception e){
            log.error("Can not send user registration event", e);
        }
    }

    public void sendPasswordResetEvent(String email, String temporaryPassword) {
        NotificationDto notification = NotificationDto.builder()
                .senderId(0L)
                .receiverId(0L) // Will be resolved by email
                .title("Password Reset")
                .message("Your password has been reset. Temporary password: " + temporaryPassword)
                .type(NotificationType.PASSWORD_RESET)
                .sendEmail(true)
                .recipientEmail(email)
                .build();

        kafkaTemplate.send("notification-topic", notification);
        log.info("Password reset notification sent to: {}", email);
    }

    /**
     * Send email notification (using EmailNotificationDto)
     * This method sends to the 'email-notifications' topic for direct email delivery
     */
    public void sendEmailNotification(EmailNotificationDto emailNotification) {
        try {
            // Set default values if not provided
            if (emailNotification.getIsHtml() == null) {
                emailNotification.setIsHtml(false);
            }

            if (emailNotification.getSenderName() == null) {
                emailNotification.setSenderName("Medical Consultation System");
            }

            // Send to email-notifications topic
            kafkaTemplate.send("email-notifications", emailNotification);

            log.info("Email notification sent to: {}", emailNotification.getRecipient());
        } catch (Exception e) {
            log.error("Failed to send email notification to: {}",
                    emailNotification.getRecipient(), e);
            throw e; // Re-throw to allow caller to handle
        }
    }

    /**
     * Send user status change notification
     */
    public void sendUserStatusChangeNotification(Long userId, String email, String fullName,
                                                 String oldStatus, String newStatus, String reason) {
        try {
            Map<String, Object> statusChangeEvent = new HashMap<>();
            statusChangeEvent.put("userId", userId);
            statusChangeEvent.put("email", email);
            statusChangeEvent.put("fullName", fullName);
            statusChangeEvent.put("oldStatus", oldStatus);
            statusChangeEvent.put("newStatus", newStatus);
            statusChangeEvent.put("reason", reason);
            statusChangeEvent.put("timestamp", System.currentTimeMillis());

            kafkaTemplate.send("user-status-change-topic", statusChangeEvent);
            log.info("User status change event sent for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send user status change event", e);
        }
    }

    /**
     * Send user deletion event
     */
    public void sendUserDeletionEvent(Long userId, String email, String role) {
        try {
            Map<String, Object> deletionEvent = new HashMap<>();
            deletionEvent.put("userId", userId);
            deletionEvent.put("email", email);
            deletionEvent.put("role", role);
            deletionEvent.put("timestamp", System.currentTimeMillis());

            kafkaTemplate.send("user-deletion-topic", deletionEvent);
            log.info("User deletion event sent for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send user deletion event", e);
        }
    }
}