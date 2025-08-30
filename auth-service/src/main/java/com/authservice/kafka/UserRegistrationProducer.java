package com.authservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String USER_REGISTRATION_TOPIC = "user-registration-topic";

    public void sendUserRegistrationEvent(Long userId, String email, String role) {
        Map<String, Object> registrationEvent = new HashMap<>();
        registrationEvent.put("userId", userId);
        registrationEvent.put("email", email);
        registrationEvent.put("role", role);
        registrationEvent.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send(USER_REGISTRATION_TOPIC, registrationEvent);
        log.info("User registration event sent for user: {}", email);
    }
}