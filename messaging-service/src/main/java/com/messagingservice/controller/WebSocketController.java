package com.messagingservice.controller;

import com.messagingservice.dto.JoinConversationDto;
import com.messagingservice.dto.MessageReadDto;
import com.messagingservice.dto.OnlineStatusDto;
import com.messagingservice.dto.TypingDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/conversation.join")
    public void joinConversation(@Payload JoinConversationDto dto) {
        log.info("User {} joining conversation {}", dto.getUserId(), dto.getConversationId());
        
        messagingTemplate.convertAndSend(
            "/topic/conversation." + dto.getConversationId() + ".status",
            new OnlineStatusDto(dto.getUserId(), true, LocalDateTime.now())
        );
    }

    @MessageMapping("/conversation.leave")
    public void leaveConversation(@Payload JoinConversationDto dto) {
        log.info("User {} leaving conversation {}", dto.getUserId(), dto.getConversationId());
        
        messagingTemplate.convertAndSend(
            "/topic/conversation." + dto.getConversationId() + ".status",
            new OnlineStatusDto(dto.getUserId(), false, LocalDateTime.now())
        );
    }

    @MessageMapping("/typing.start")
    public void typingStart(@Payload TypingDto dto) {
        log.debug("User {} started typing in conversation {}", dto.getUserId(), dto.getConversationId());
        
        dto.setIsTyping(true);
        messagingTemplate.convertAndSend(
            "/topic/conversation." + dto.getConversationId() + ".typing",
            dto
        );
    }

    @MessageMapping("/typing.stop")
    public void typingStop(@Payload TypingDto dto) {
        log.debug("User {} stopped typing in conversation {}", dto.getUserId(), dto.getConversationId());
        
        dto.setIsTyping(false);
        messagingTemplate.convertAndSend(
            "/topic/conversation." + dto.getConversationId() + ".typing",
            dto
        );
    }

    public void broadcastNewMessage(Long conversationId, Object message) {
        log.info("Broadcasting new message to conversation {}", conversationId);
        
        messagingTemplate.convertAndSend(
            "/topic/conversation." + conversationId + ".messages",
            message
        );
    }

    public void broadcastMessageRead(Long conversationId, Long messageId, LocalDateTime readAt) {
        log.debug("Broadcasting message read status for message {}", messageId);
        
        messagingTemplate.convertAndSend(
            "/topic/conversation." + conversationId + ".read",
            new MessageReadDto(messageId, readAt)
        );
    }
}
