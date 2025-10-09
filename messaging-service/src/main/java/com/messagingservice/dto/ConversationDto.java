package com.messagingservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDto {
    
    private Long id;
    private Long patientId;
    private Long doctorId;
    private Long caseId;
    private String title;
    private String status;
    private Long lastMessageId;
    private LocalDateTime lastMessageAt;
    private String lastMessagePreview;
    private Integer unreadCount;
    private Integer totalMessagesCount;
    private String patientName;
    private String doctorName;
    private String otherUserName;
    private Long otherUserId;
    private Boolean isOnline;
    private LocalDateTime createdAt;
}