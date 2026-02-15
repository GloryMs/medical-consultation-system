package com.notificationservice.service;

import com.commonlibrary.dto.NotificationDto;
import com.notificationservice.entity.Notification;
import com.commonlibrary.entity.NotificationPriority;
import com.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.commonlibrary.entity.UserType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    @Transactional
    public Notification createNotification(NotificationDto dto) {

        // Validate that new fields are provided
        if (dto.getReceiverUserId() == null || dto.getReceiverType() == null) {
            log.error("Missing receiverUserId or receiverType in notification: {}", dto.getTitle());
            throw new IllegalArgumentException("receiverUserId and receiverType are required");
        }

        if (dto.getSenderUserId() == null || dto.getSenderType() == null) {
            log.warn("Missing senderUserId or senderType, using SYSTEM default");
            dto.setSenderUserId(0L);
            dto.setSenderType(UserType.SYSTEM);
        }

        Notification notification = Notification.builder()
                .senderUserId(dto.getSenderUserId())
                .receiverUserId(dto.getReceiverUserId())
                .senderType(dto.getSenderType())
                .receiverType(dto.getReceiverType())
                //.senderId(dto.getSenderId())
                //.receiverId(dto.getReceiverId())
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

    // NEW METHOD - With userId and UserType
    public List<NotificationDto> getUserNotifications(Long userId, UserType userType) {
        return notificationRepository
                .findByReceiverUserIdAndReceiverTypeOrderByCreatedAtDesc(userId, userType)
                .stream()
                .map(this::convertToNotificationDto)
                .collect(Collectors.toList());
    }

    // NEW METHOD - With userId and UserType
    public List<NotificationDto> getUnreadNotifications(Long userId, UserType userType) {
        return notificationRepository
                .findByReceiverUserIdAndReceiverTypeAndIsReadFalseOrderByCreatedAtDesc(userId, userType)
                .stream()
                .map(this::convertToNotificationDto)
                .collect(Collectors.toList());
    }

    public List<NotificationDto> getUserNotifications(Long userId) {
        return notificationRepository.findByReceiverIdOrderByCreatedAtDesc(userId).stream().
                map(this::convertToNotificationDto).collect(Collectors.toList());
    }

    public List<NotificationDto> getUnreadNotifications(Long userId) {
        return notificationRepository.findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(userId).stream().
                map(this::convertToNotificationDto).collect(Collectors.toList());
    }

    public NotificationDto convertToNotificationDto(Notification notification) {
        NotificationDto notificationDto = new NotificationDto();
        ModelMapper modelMapper = new ModelMapper();
        notificationDto = modelMapper.map(notification, NotificationDto.class);
        return notificationDto;
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId, UserType userType) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // Verify ownership with both userId and userType
        if (!notification.getReceiverUserId().equals(userId) ||
                !notification.getReceiverType().equals(userType)) {
            throw new RuntimeException("Unauthorized access to notification");
        }

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId, UserType userType) {
        List<Notification> unreadNotifications = notificationRepository
                .findByReceiverUserIdAndReceiverTypeAndIsReadFalseOrderByCreatedAtDesc(userId, userType);

        unreadNotifications.forEach(notification -> {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
        });

        notificationRepository.saveAll(unreadNotifications);
    }

    @Deprecated
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // Verify ownership with both userId and userType
        if (!notification.getReceiverUserId().equals(userId) ) {
            throw new RuntimeException("Unauthorized access to notification");
        }

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Deprecated
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
