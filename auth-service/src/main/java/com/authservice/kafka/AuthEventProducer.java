package com.authservice.kafka;

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
}