package com.messagingservice.service;

import com.commonlibrary.dto.NotificationDto;
import com.commonlibrary.entity.NotificationPriority;
import com.commonlibrary.entity.NotificationType;
import com.messagingservice.dto.MessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageNotificationService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    // TODO: Add NotificationServiceClient when integrating with notification service
    // private final NotificationServiceClient notificationServiceClient;

    /**
     * Send notification for new message
     */
    @Async
    public void notifyNewMessage(MessageDto message) {
        try {
            NotificationDto notification = NotificationDto.builder()
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .type(NotificationType.NEW_MESSAGE)
                .title("New Message")
                .message("You have a new message from " + message.getSenderName())
                .actionUrl("/app/communication/conversation/" + message.getConversationId())
                .priority(NotificationPriority.MEDIUM)
                .build();

            // Send via Kafka
            kafkaTemplate.send("notification-events", notification);
            
            log.info("Notification sent for new message {}", message.getId());
            
            // TODO: Uncomment when notification service is integrated
            // notificationServiceClient.sendNotification(notification);
        } catch (Exception e) {
            log.error("Failed to send new message notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send notification for message read
     */
    @Async
    public void notifyMessageRead(Long messageId, Long senderId, String senderName) {
        try {
            log.info("Message {} read by {}", messageId, senderName);
            // Optionally send read receipts via WebSocket or notification
        } catch (Exception e) {
            log.error("Failed to send message read notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send notification for unread messages
     */
    @Async
    public void notifyUnreadMessages(Long userId, Integer unreadCount) {
        try {
            if (unreadCount > 0) {
                NotificationDto notification = NotificationDto.builder()
                    .receiverId(userId)
                    .type(NotificationType.REMINDER)
                    .title("Unread Messages")
                    .message("You have " + unreadCount + " unread message(s)")
                    .actionUrl("/app/communication")
                    .priority(NotificationPriority.LOW)
                    .build();

                kafkaTemplate.send("notification-events", notification);
                
                log.info("Unread message notification sent to user {}", userId);
            }
        } catch (Exception e) {
                            log.error("Failed to send unread messages notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send email notification for important messages
     */
    @Async
    public void sendEmailNotification(MessageDto message, String recipientEmail) {
        try {
            if (recipientEmail != null && !recipientEmail.isEmpty()) {
                NotificationDto notification = NotificationDto.builder()
                    .senderId(message.getSenderId())
                    .receiverId(message.getReceiverId())
                    .type(NotificationType.NEW_MESSAGE)
                    .title("New Message in Medical Consultation System")
                    .message("You have received a new message regarding Case #" + message.getCaseId())
                    .recipientEmail(recipientEmail)
                    .sendEmail(true)
                    .priority(NotificationPriority.HIGH)
                    .build();

                kafkaTemplate.send("notification-events", notification);
                
                log.info("Email notification queued for {}", recipientEmail);
            }
        } catch (Exception e) {
            log.error("Failed to send email notification: {}", e.getMessage(), e);
        }
    }
}