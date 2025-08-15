package com.notificationservice.dto;

import com.notificationservice.entity.NotificationPriority;
import com.notificationservice.entity.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationDto {
    @NotNull
    private Long senderId;

    @NotNull
    private Long receiverId;

    @NotNull
    private NotificationType type;

    @NotNull
    private String title;

    @NotNull
    private String message;

    private String actionUrl;

    private NotificationPriority priority;

    private String recipientEmail;

    private Boolean sendEmail;
}
