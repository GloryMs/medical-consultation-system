package com.notificationservice.kafka;

import com.notificationservice.dto.NotificationDto;
import com.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "notification-topic", groupId = "notification-group")
    public void consumeNotification(NotificationDto notificationDto) {
        log.info("Received notification event: {}", notificationDto.getTitle());
        notificationService.createNotification(notificationDto);
    }
}
