package com.messagingservice.dto;

import com.commonlibrary.entity.UserRole;
import com.messagingservice.entity.MessageStatus;
import com.messagingservice.entity.MessageType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {
    
    private Long id;
    private Long conversationId;
    private Long senderId;
    private Long receiverId;
    private UserRole senderRole;
    private UserRole receiverRole;
    private Long caseId;
    private String content;
    private MessageType messageType;
    private Boolean isRead;
    private LocalDateTime readAt;
    private MessageStatus status;
    private Long replyToMessageId;
    private String senderName;
    private String receiverName;
    private LocalDateTime createdAt;
    private List<MessageAttachmentDto> attachments;
}