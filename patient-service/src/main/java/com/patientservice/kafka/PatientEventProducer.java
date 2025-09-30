package com.patientservice.kafka;

import com.commonlibrary.dto.NotificationDto;
import com.commonlibrary.entity.NotificationType;
import com.patientservice.service.SmartCaseAssignmentService;
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
public class PatientEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendCaseStatusUpdateEvent(Long caseId, String oldStatus, String newStatus, 
                                        Long patientId, Long doctorId) {
        // Send notification to patient
        NotificationDto patientNotification = NotificationDto.builder()
                .senderId(doctorId != null ? doctorId : 0L)
                .receiverId(patientId)
                .title("Case Status Updated")
                .message("Your case #" + caseId + " status has been updated to: " + newStatus)
                .type(NotificationType.CASE)
                .sendEmail(true)
                .build();

        kafkaTemplate.send("notification-topic", patientNotification);

        // Send case status event
        Map<String, Object> caseEvent = new HashMap<>();
        caseEvent.put("caseId", caseId);
        caseEvent.put("oldStatus", oldStatus);
        caseEvent.put("newStatus", newStatus);
        caseEvent.put("patientId", patientId);
        caseEvent.put("doctorId", doctorId);
        caseEvent.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send("case-status-updated-topic", caseEvent);
        log.info("Kafka - Case status updated event sent for case: {}", caseId);
    }

    public void sendSubscriptionCreatedEvent(Long patientId, String planType, Double amount) {
        NotificationDto notification = NotificationDto.builder()
                .senderId(0L)
                .receiverId(patientId)
                .title("Subscription Created")
                .message("Your " + planType + " subscription has been created successfully for $" + amount)
                .type(NotificationType.SUBSCRIPTION)
                .sendEmail(true)
                .build();

        kafkaTemplate.send("notification-topic", notification);
        log.info("Kafka - Subscription notification sent for patient: {}", patientId);
    }

    public void sendStartSmartCaseAssignmentService(Long caseId){

        Map<String, Object> caseEvent = new HashMap<>();
        caseEvent.put("caseId", caseId);
        caseEvent.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send("case-create-assign-topic", caseEvent);
        log.info("Kafka - Trigger SmartCaseAssignmentService for case: {}", caseId);

    }

    public void sendUpdateDoctorWorkLoadTrigger(Long doctorId){
        Map<String, Object> doctorEvent = new HashMap<>();
        doctorEvent.put("doctorId", doctorId);
        doctorEvent.put("timestamp", System.currentTimeMillis());
        kafkaTemplate.send("case-update-doctor-workload-topic", doctorEvent);
        log.info("Kafka - Trigger update doctor workload for doctor: {}", doctorId);
    }

    public void sendRescheduleRequestEvent(Long caseId, Long patientId, Long doctorId, String reason) {
        NotificationDto doctorNotification = NotificationDto.builder()
                .senderId(patientId)
                .receiverId(doctorId)
                .title("Reschedule Request")
                .message("Patient requested to reschedule case #" + caseId + ". Reason: " + reason)
                .type(NotificationType.SUBSCRIPTION)
                .sendEmail(true)
                .build();

        kafkaTemplate.send("notification-topic", doctorNotification);
        log.info("Kafka - Reschedule request notification sent to doctor: {}", doctorId);
    }

    public void sendAssignmentNotification(Long senderId, Long receiverId,
                                           Long caseId, String title, String message) {
        NotificationDto doctorNotification = NotificationDto.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .title(title)
                .message(message)
                .type(NotificationType.CASE)
                .sendEmail(true)
                .build();

        kafkaTemplate.send("notification-topic", doctorNotification);
        log.info("Kafka - Assignment Notification for case {}", caseId);
    }

    public void sendScheduleConfirmationEvent(Long caseId, Long patientId, Long doctorId) {
        // Send notification to Doctor
        NotificationDto patientNotification = NotificationDto.builder()
                .senderId(patientId != null ? patientId : 0L)
                .receiverId(doctorId)
                .title("Case appointment Confirmation")
                .message("Patient has confirmed the appointment for case #" + caseId )
                .type(NotificationType.APPOINTMENT)
                .sendEmail(true)
                .build();

        kafkaTemplate.send("notification-topic", patientNotification);

        // Send case status event
        Map<String, Object> appointmentConfirmationEvent = new HashMap<>();
        appointmentConfirmationEvent.put("caseId", caseId);
        appointmentConfirmationEvent.put("patientId", patientId);
        appointmentConfirmationEvent.put("doctorId", doctorId);
        appointmentConfirmationEvent.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send("case-appointment-confirmation-topic", appointmentConfirmationEvent);
        log.info("Kafka - Appointment confirmation for case: {} , by patient {}, and doctor", caseId, patientId);
    }
}