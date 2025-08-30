package com.commonlibrary.kafka;

import com.commonlibrary.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String NOTIFICATION_TOPIC = "notification-topic";

    public void sendNotification(NotificationDto notification) {
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(NOTIFICATION_TOPIC, notification);
            
            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    log.info("Sent notification=[{}] with offset=[{}]", 
                            notification.getTitle(), result.getRecordMetadata().offset());
                } else {
                    log.error("Unable to send notification=[{}] due to : {}", 
                            notification.getTitle(), exception.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Error sending notification: {}", e.getMessage());
        }
    }
}