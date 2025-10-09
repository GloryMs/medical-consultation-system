package com.messagingservice.dto;

import lombok.Data;

@Data
public class JoinConversationDto {
    private Long conversationId;
    private Long userId;
}
