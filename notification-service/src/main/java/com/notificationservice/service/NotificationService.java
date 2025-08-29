package com.notificationservice.service;

import com.commonlibrary.dto.NotificationDto;
import com.notificationservice.entity.Notification;
import com.commonlibrary.entity.NotificationPriority;
import com.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    @Transactional
    public Notification createNotification(NotificationDto dto) {
        Notification notification = Notification.builder()
                .senderId(dto.getSenderId())
                .receiverId(dto.getReceiverId())
                .type(dto.getType())
                .title(dto.getTitle())
                .message(dto.getMessage())
                .actionUrl(dto.getActionUrl())
                .priority(dto.getPriority() != null ? dto.getPriority() : NotificationPriority.MEDIUM)
                .recipientEmail(dto.getRecipientEmail())
                .isRead(false)
                .emailSent(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        // Send email asynchronously if required
        if (Boolean.TRUE.equals(dto.getSendEmail()) && dto.getRecipientEmail() != null) {
            sendEmailNotification(saved);
        }

        log.info("Notification created: {} for user {}", dto.getTitle(), dto.getReceiverId());

        return saved;
    }

    @Async
    protected void sendEmailNotification(Notification notification) {
        try {
            emailService.sendEmail(
                    notification.getRecipientEmail(),
                    notification.getTitle(),
                    notification.getMessage()
            );
            notification.setEmailSent(true);
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("Failed to send email notification: {}", e.getMessage());
        }
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByReceiverIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getReceiverId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to notification");
        }

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository
                .findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(userId);

        unreadNotifications.forEach(notification -> {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
        });

        notificationRepository.saveAll(unreadNotifications);
    }
}
