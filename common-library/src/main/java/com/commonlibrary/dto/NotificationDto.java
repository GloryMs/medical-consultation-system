package com.commonlibrary.dto;

import com.commonlibrary.entity.NotificationPriority;
import com.commonlibrary.entity.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private NotificationType type;
    private String title;
    private String message;
    private String actionUrl;
    private NotificationPriority priority;
    private String recipientEmail;
    private String recipientPhone;
    private Boolean sendEmail;
    private Boolean isRead;
    private Boolean emailSent;
    private LocalDateTime readAt;
}
