package com.messagingservice.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountDto {
    
    private Long totalUnread;
    private List<ConversationUnreadDto> conversationUnreads;
}

