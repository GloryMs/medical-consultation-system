package com.doctorservice.kafka;

import com.commonlibrary.dto.NotificationDto;
import com.commonlibrary.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DoctorEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void SendCaseScheduleUpdate(Long patientId, Long caseId,
                                       LocalDateTime appointmentTime, String doctorName) {
        // Send notification to notification service
        NotificationDto notification = NotificationDto.builder()
                .senderId(0L) // System notification
                .receiverId(patientId)
                .title("Appointment Scheduling")
                .message("An appointment has been scheduled for your case# " + caseId +
                        " at: " + appointmentTime +" with Doctor  " + doctorName )
                .type(NotificationType.APPOINTMENT)
                .sendEmail(true)
                .build();

        kafkaTemplate.send("notification-topic", notification);
        log.info("Payment completion notification sent for patient: {}", patientId);
    }
}