package com.paymentservice.kafka;

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
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPaymentCompletedEvent(Long patientId, Long doctorId, Long caseId,
                                          String paymentType, Double amount, String transactionId) {
        // Send notification to notification service
        NotificationDto notification = NotificationDto.builder()
                .senderId(0L) // System notification
                .receiverId(patientId)
                .title("Payment Successful")
                .message("Your " + paymentType + " payment of $" + amount + " has been processed successfully. Transaction ID: " + transactionId)
                .type(NotificationType.PAYMENT)
                .sendEmail(true)
                .build();

        kafkaTemplate.send("notification-topic", notification);
        log.info("Payment completion notification sent for patient: {}", patientId);

        // Send payment event for other services
        Map<String, Object> paymentEvent = new HashMap<>();
        paymentEvent.put("patientId", patientId);
        paymentEvent.put("doctorId", doctorId);
        paymentEvent.put("caseId", caseId);
        paymentEvent.put("paymentType", paymentType);
        paymentEvent.put("amount", amount);
        paymentEvent.put("transactionId", transactionId);
        paymentEvent.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send("payment-completed-topic", paymentEvent);
        log.info("Payment event sent for case: {}", caseId);
    }
}