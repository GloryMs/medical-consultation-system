package com.messagingservice.controller;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.entity.UserRole;
import com.messagingservice.dto.*;
import com.messagingservice.entity.ConversationStatus;
import com.messagingservice.service.ConversationService;
import com.messagingservice.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final ConversationService conversationService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<MessageDto>> sendMessage(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") UserRole userRole,
            @Valid @RequestBody SendMessageDto dto) {

        MessageDto message = messageService.sendMessage(userId, userRole, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, "Message sent successfully"));
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<ApiResponse<List<MessageDto>>> getConversationMessages(
            @PathVariable Long conversationId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        List<MessageDto> messages = messageService.getConversationMessages(
                conversationId, userId, page, size
        );
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @PutMapping("/{messageId}/read")
    public ResponseEntity<ApiResponse<Void>> markMessageAsRead(
            @PathVariable Long messageId,
            @RequestHeader("X-User-Id") Long userId) {

        messageService.markMessageAsRead(messageId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Message marked as read"));
    }

    @PutMapping("/conversations/{conversationId}/mark-read")
    public ResponseEntity<ApiResponse<Void>> markConversationAsRead(
            @PathVariable Long conversationId,
            @RequestHeader("X-User-Id") Long userId) {

        messageService.markConversationAsRead(conversationId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Conversation marked as read"));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountDto>> getUnreadCount(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") UserRole userRole) {

        UnreadCountDto unreadCount = messageService.getUnreadCount(userId, userRole);
        return ResponseEntity.ok(ApiResponse.success(unreadCount));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<MessageDto>>> searchMessages(
            @RequestParam Long conversationId,
            @RequestParam String query,
            @RequestHeader("X-User-Id") Long userId) {

        List<MessageDto> messages = messageService.searchMessages(conversationId, query, userId);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @PathVariable Long messageId,
            @RequestHeader("X-User-Id") Long userId) {

        messageService.deleteMessage(messageId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Message deleted"));
    }

    @PostMapping("/conversations")
    public ResponseEntity<ApiResponse<ConversationDto>> createConversation(
            @Valid @RequestBody CreateConversationDto dto) {

        ConversationDto conversation = conversationService.createConversation(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(conversation, "Conversation created"));
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ApiResponse<ConversationDto>> getConversation(
            @PathVariable Long conversationId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") UserRole userRole) {

        ConversationDto conversation = conversationService.getConversationById(
                conversationId, userId, userRole
        );
        return ResponseEntity.ok(ApiResponse.success(conversation));
    }

    @GetMapping("/conversations/case/{caseId}")
    public ResponseEntity<ApiResponse<ConversationDto>> getConversationByCase(
            @PathVariable Long caseId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") UserRole userRole) {

        ConversationDto conversation = conversationService.getConversationByCase(
                caseId, userId, userRole
        );
        return ResponseEntity.ok(ApiResponse.success(conversation));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationDto>>> getUserConversations(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") UserRole userRole,
            @RequestParam(required = false) ConversationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<ConversationDto> conversations = conversationService.getUserConversations(
                userId, userRole, status, page, size
        );
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    @PutMapping("/conversations/{conversationId}/status")
    public ResponseEntity<ApiResponse<Void>> updateConversationStatus(
            @PathVariable Long conversationId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam ConversationStatus status) {

        conversationService.updateConversationStatus(conversationId, userId, status);
        return ResponseEntity.ok(ApiResponse.success(null, "Conversation status updated"));
    }

    @PostMapping("/conversations/{conversationId}/archive")
    public ResponseEntity<ApiResponse<Void>> archiveConversation(
            @PathVariable Long conversationId,
            @RequestHeader("X-User-Id") Long userId) {

        conversationService.archiveConversation(conversationId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Conversation archived"));
    }

    @GetMapping("/conversations/search")
    public ResponseEntity<ApiResponse<List<ConversationDto>>> searchConversations(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") UserRole userRole,
            @RequestParam String query) {

        List<ConversationDto> conversations = conversationService.searchConversations(
                userId, userRole, query
        );
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }
}