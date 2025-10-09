package com.messagingservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OnlineStatusDto {
    private Long userId;
    private boolean isOnline;
    private LocalDateTime lastSeen;

    public OnlineStatusDto(Long userId, boolean b, LocalDateTime now) {
    }
}
