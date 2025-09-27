package com.doctorservice.kafka;

import com.commonlibrary.dto.CaseFeeUpdateEvent;
import com.commonlibrary.dto.NotificationDto;
import com.commonlibrary.entity.NotificationPriority;
import com.commonlibrary.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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

    /**
     * Send case fee update event via Kafka
     */
    public void sendCaseFeeUpdateEvent(Long caseId, Long doctorId, Long doctorUserId, Long patientId,
                                       Long patientUserId, BigDecimal consultationFee) {
        try {
            // Create case fee update event
            CaseFeeUpdateEvent feeEvent = CaseFeeUpdateEvent.builder()
                    .caseId(caseId)
                    .doctorId(doctorId)
                    .doctorUserId(doctorUserId)
                    .patientId(patientId)
                    .patientUserId(patientUserId)
                    .consultationFee(consultationFee)
                    .feeSetAt(LocalDateTime.now())
                    .eventType("CASE_FEE_SET")
                    .timestamp(System.currentTimeMillis())
                    .build();

            // Send event to update case in patient service
            kafkaTemplate.send("case-fee-update-topic", feeEvent);
            log.info("Case fee update event sent for case: {} with fee: ${}", caseId, consultationFee);

            // Send notification to patient about fee being set
            NotificationDto patientNotification = NotificationDto.builder()
                    .senderId(doctorUserId)
                    .receiverId(patientUserId)
                    .title("Consultation Fee Set")
                    .message(String.format("Your doctor has set the consultation fee of $%.2f for your case. You can now proceed to schedule your appointment.", consultationFee))
                    .type(NotificationType.CASE)
                    .sendEmail(true)
                    .priority(NotificationPriority.MEDIUM)
                    .build();

            kafkaTemplate.send("notification-topic", patientNotification);
            log.info("Case fee notification sent to patient: {}", patientId);

        } catch (Exception e) {
            log.error("Error sending case fee update event for case {}: {}", caseId, e.getMessage(), e);
        }
    }
}