// 1. AdminEventProducer class
package com.adminservice.kafka;

import com.adminservice.feign.DoctorServiceClient;
import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.dto.DoctorStatusEvent;
import com.commonlibrary.dto.NotificationDto;
import com.commonlibrary.entity.NotificationPriority;
import com.commonlibrary.entity.NotificationType;
import com.commonlibrary.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import com.commonlibrary.entity.UserType;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final DoctorServiceClient doctorService;

    /**
     * Send notification to admin when new doctor registers
     */
    public void sendDoctorOrSupervisorRegistrationNotification(Long userId, String role,
                                                               String email, Long adminUserId) {
        try {
            NotificationDto adminNotification = NotificationDto.builder()
                    .senderUserId(0L) // System notification
                    .receiverUserId(1L)
                    .senderType(UserType.SYSTEM)
                    .receiverType(UserType.ADMIN)
                    .title("New " + role +" Registration - Action Required")
                    .message(String.format(
                        "A new " + role +" has registered and requires your review:\n\n" +
                        "Email: %s\n" +
                        "User ID: %s\n\n" +
                        "Please review their credentials and verify their account to activate their profile.",
                            email,
                            userId
                    ))
                    .type(NotificationType.DOCTOR_REGISTRATION)
                    .sendEmail(true)
                    .priority(NotificationPriority.HIGH)
                    .build();

            kafkaTemplate.send("notification-topic", adminNotification);
            log.info( role +"  registration notification sent to admin {} for "+ role +": {}",
                    adminUserId, email);

        } catch (Exception e) {
            log.error("Error sending "+ role +" registration notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send notification to admin when doctor profile needs verification
     */
    public void sendDoctorVerificationReminder(Long doctorUserId, String doctorEmail, 
                                             String doctorName, Long adminUserId) {
        try {
            NotificationDto reminderNotification = NotificationDto.builder()
                    .senderUserId(0L) // System notification
                    .receiverUserId(1L)
                    .senderType(UserType.SYSTEM)
                    .receiverType(UserType.ADMIN)
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
    public void sendDoctorStatusChangeNotification(Long doctorId, String doctorEmail,
                                                 String doctorName, String newStatus, 
                                                 String reason, Boolean approved) {
        try {
            NotificationDto statusNotification = NotificationDto.builder()
                    .senderUserId(0L) // System notification
                    .receiverUserId(getDoctorUserId(doctorId))
                    .senderType(UserType.SYSTEM)
                    .receiverType(UserType.DOCTOR)
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

        // Send Event for Auth-Service to update doctor's record in User's table
        try {

            DoctorStatusEvent doctorStatusEvent = new DoctorStatusEvent();
            doctorStatusEvent.setDoctorEmail(doctorEmail);
            doctorStatusEvent.setNewStatus(newStatus);
            doctorStatusEvent.setApproved(approved);

            kafkaTemplate.send("auth-system-events-topic", doctorStatusEvent);
            log.info("Auth system event sent: update doctor (email {}) account verification status , to {}" ,
                    doctorEmail, newStatus);

        } catch (Exception e) {
            log.error("Error sending admin system event: {}", e.getMessage(), e);
            e.printStackTrace();
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
                    .senderUserId(0L) // System notification
                    .receiverUserId(1L)
                    .senderType(UserType.SYSTEM)
                    .receiverType(UserType.ADMIN)
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

    private Long getDoctorUserId(Long doctorId) {
        Long doctorUserId = null;
        try {
            ApiResponse<?> response = doctorService.getDoctorById(doctorId).getBody();
            if (response == null || response.getData() == null) {
                throw new BusinessException("Doctor not found", HttpStatus.NOT_FOUND);
            }
            Map<String, Object> doctorData = (Map<String, Object>) response.getData();
            doctorUserId = Long.parseLong(doctorData.get("userId").toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return doctorUserId;
    }
}