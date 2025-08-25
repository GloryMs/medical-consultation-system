package com.notificationservice.dto;

import com.notificationservice.entity.NotificationPriority;
import com.notificationservice.entity.NotificationType;
import lombok.Data;

@Data
public class NotificationDto {
    private Long senderId;
    private Long receiverId;
    private NotificationType type;
    private String title;
    private String message;
    private String actionUrl;
    private NotificationPriority priority;
    private String recipientEmail;
    private Boolean sendEmail;
}
