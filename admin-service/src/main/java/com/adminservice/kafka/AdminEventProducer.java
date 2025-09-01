// 1. AdminEventProducer class
package com.adminservice.kafka;

import com.commonlibrary.dto.NotificationDto;
import com.commonlibrary.entity.NotificationPriority;
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
public class AdminEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Send notification to admin when new doctor registers
     */
    public void sendDoctorRegistrationNotification(Long doctorUserId, String doctorEmail, Long adminUserId) {
        try {
            NotificationDto adminNotification = NotificationDto.builder()
                    .senderId(0L) // System notification
                    .receiverId(adminUserId)
                    .title("New Doctor Registration - Action Required")
                    .message(String.format(
                        "A new doctor has registered and requires your review:\n\n" +
                        "Email: %s\n" +
                        "User ID: %s\n\n" +
                        "Please review their credentials and verify their account to activate their profile.",
                        doctorEmail,
                        doctorUserId
                    ))
                    .type(NotificationType.DOCTOR_REGISTRATION)
                    .sendEmail(true)
                    .priority(NotificationPriority.HIGH)
                    .build();

            kafkaTemplate.send("notification-topic", adminNotification);
            log.info("Doctor registration notification sent to admin {} for doctor: {}", 
                    adminUserId, doctorEmail);

        } catch (Exception e) {
            log.error("Error sending doctor registration notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send notification to admin when doctor profile needs verification
     */
    public void sendDoctorVerificationReminder(Long doctorUserId, String doctorEmail, 
                                             String doctorName, Long adminUserId) {
        try {
            NotificationDto reminderNotification = NotificationDto.builder()
                    .senderId(0L)
                    .receiverId(adminUserId)
                    .title("Doctor Verification Reminder")
                    .message(String.format(
                        "Doctor profile is still pending verification:\n\n" +
                        "Doctor: %s (%s)\n" +
                        "Registration Date: Pending review\n\n" +
                        "Please complete the verification process to activate their account.",
                        doctorName != null ? doctorName : doctorEmail,
                        doctorEmail
                    ))
                    .type(NotificationType.DOCTOR_VERIFICATION_REMINDER)
                    .sendEmail(true)
                    .priority(NotificationPriority.MEDIUM)
                    .build();

            kafkaTemplate.send("notification-topic", reminderNotification);
            log.info("Doctor verification reminder sent to admin {} for doctor: {}", 
                    adminUserId, doctorEmail);

        } catch (Exception e) {
            log.error("Error sending doctor verification reminder: {}", e.getMessage(), e);
        }
    }

    /**
     * Send notification when doctor status changes (approved/rejected)
     */
    public void sendDoctorStatusChangeNotification(Long doctorUserId, String doctorEmail, 
                                                 String doctorName, String newStatus, 
                                                 String reason) {
        try {
            NotificationDto statusNotification = NotificationDto.builder()
                    .senderId(0L)
                    .receiverId(doctorUserId) // Send to doctor
                    .title("Account Status Update")
                    .message(String.format(
                        "Hello Dr. %s,\n\n" +
                        "Your account status has been updated to: %s\n\n" +
                        "%s\n\n" +
                        "For questions, please contact our support team.",
                        doctorName != null ? doctorName : "Doctor",
                        newStatus,
                        reason != null ? "Reason: " + reason : ""
                    ))
                    .type(NotificationType.DOCTOR_STATUS_CHANGE)
                    .sendEmail(true)
                    .recipientEmail(doctorEmail)
                    .priority(NotificationPriority.HIGH)
                    .build();

            kafkaTemplate.send("notification-topic", statusNotification);
            log.info("Doctor status change notification sent to doctor: {} ({})", 
                    doctorEmail, newStatus);

        } catch (Exception e) {
            log.error("Error sending doctor status change notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send urgent system notifications to admin
     */
    public void sendUrgentAdminNotification(Long adminUserId, String title, String message, 
                                          String type) {
        try {
            NotificationType notificationType = NotificationType.valueOf(type);
            NotificationDto urgentNotification = NotificationDto.builder()
                    .senderId(0L)
                    .receiverId(adminUserId)
                    .title(title)
                    .message(message)
                    .type(notificationType)
                    .sendEmail(true)
                    .priority(NotificationPriority.HIGH)
                    .build();

            kafkaTemplate.send("notification-topic", urgentNotification);
            log.info("Urgent admin notification sent: {}", title);

        } catch (Exception e) {
            log.error("Error sending urgent admin notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send admin event for system monitoring
     */
    public void sendAdminSystemEvent(String eventType, Map<String, Object> eventData) {
        try {
            Map<String, Object> systemEvent = new HashMap<>();
            systemEvent.put("eventType", eventType);
            systemEvent.put("timestamp", System.currentTimeMillis());
            systemEvent.put("data", eventData);

            kafkaTemplate.send("admin-system-events", systemEvent);
            log.info("Admin system event sent: {}", eventType);

        } catch (Exception e) {
            log.error("Error sending admin system event: {}", e.getMessage(), e);
        }
    }
}