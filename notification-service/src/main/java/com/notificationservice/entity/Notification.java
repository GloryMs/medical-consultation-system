package com.notificationservice.entity;

import com.commonlibrary.entity.BaseEntity;
import com.commonlibrary.entity.NotificationPriority;
import com.commonlibrary.entity.NotificationType;
import com.commonlibrary.entity.UserType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseEntity {

    // NEW FIELDS - Use userId from auth service
    @Column(name = "sender_user_id", nullable = false)
    private Long senderUserId;

    @Column(name = "receiver_user_id", nullable = false)
    private Long receiverUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 20)
    private UserType senderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "receiver_type", nullable = false, length = 20)
    private UserType receiverType;

    @Deprecated
    @Column(nullable = false)
    private Long senderId;

    @Deprecated
    @Column(nullable = false)
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(nullable = false)
    private Boolean isRead = false;

    @Column(nullable = false)
    private Boolean emailSent = false;

    private LocalDateTime readAt;

    private String actionUrl;

    @Enumerated(EnumType.STRING)
    private NotificationPriority priority;

    private String recipientEmail;
    private String recipientPhone;
}
