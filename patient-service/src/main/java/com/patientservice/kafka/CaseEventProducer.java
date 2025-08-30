package com.patientservice.kafka;

import com.commonlibrary.entity.NotificationType;
import com.commonlibrary.kafka.NotificationProducer;
import com.commonlibrary.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CaseEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final NotificationProducer notificationProducer;
    
    private static final String CASE_STATUS_TOPIC = "case-status-updated-topic";

    public void sendCaseStatusUpdateEvent(Long caseId, String oldStatus, 
                                        String newStatus, Long patientId, Long doctorId) {
        // Send case status event
        Map<String, Object> caseEvent = new HashMap<>();
        caseEvent.put("caseId", caseId);
        caseEvent.put("oldStatus", oldStatus);
        caseEvent.put("newStatus", newStatus);
        caseEvent.put("patientId", patientId);
        caseEvent.put("doctorId", doctorId);
        caseEvent.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send(CASE_STATUS_TOPIC, caseEvent);
        log.info("Case status updated event sent for case: {}", caseId);

        // Also send notification
        NotificationDto notification = NotificationDto.builder()
                .senderId(doctorId)
                .receiverId(patientId)
                .title("Case Status Updated")
                .message("Your case status has been updated to: " + newStatus)
                .type(NotificationType.CASE)
                .sendEmail(true)
                .build();

        notificationProducer.sendNotification(notification);
    }
}