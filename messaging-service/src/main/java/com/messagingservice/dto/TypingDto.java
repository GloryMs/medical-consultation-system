package com.messagingservice.dto;

import lombok.Data;

@Data
public class TypingDto {
    private Long conversationId;
    private Long userId;
    private String userName;
    private Boolean isTyping;
}
