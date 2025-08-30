package com.paymentservice.kafka;

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
    private static final String PAYMENT_TOPIC = "payment-completed-topic";

    public void sendPaymentCompletedEvent(Long patientId, Long doctorId, 
                                        Long caseId, String paymentType, 
                                        Double amount) {
        Map<String, Object> paymentEvent = new HashMap<>();
        paymentEvent.put("patientId", patientId);
        paymentEvent.put("doctorId", doctorId);
        paymentEvent.put("caseId", caseId);
        paymentEvent.put("paymentType", paymentType);
        paymentEvent.put("amount", amount);
        paymentEvent.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send(PAYMENT_TOPIC, paymentEvent);
        log.info("Payment completed event sent for case: {}", caseId);
    }
}