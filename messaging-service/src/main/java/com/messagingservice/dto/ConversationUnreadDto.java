package com.messagingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationUnreadDto {
    private Long conversationId;
    private Long caseId;
    private Integer unreadCount;
}
