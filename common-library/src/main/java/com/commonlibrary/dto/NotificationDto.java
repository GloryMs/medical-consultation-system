package com.commonlibrary.dto;

import com.commonlibrary.entity.NotificationPriority;
import com.commonlibrary.entity.NotificationType;
import com.commonlibrary.entity.UserType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {

    // NEW FIELDS
    private Long senderUserId;
    private Long receiverUserId;
    private UserType senderType;
    private UserType receiverType;

    private Long id;

    @Deprecated
    private Long senderId;
    @Deprecated
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
    private LocalDateTime createdAt;
}
