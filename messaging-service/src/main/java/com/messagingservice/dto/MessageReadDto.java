package com.messagingservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageReadDto {
    private Long messageId;
    private LocalDateTime readAt;

    public MessageReadDto(Long messageId, LocalDateTime readAt) {
    }
}
