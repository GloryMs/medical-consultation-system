package com.messagingservice.service;

import com.commonlibrary.entity.UserRole;
import com.commonlibrary.exception.BusinessException;
import com.messagingservice.dto.*;
import com.messagingservice.entity.*;
import com.messagingservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final MessageAttachmentRepository attachmentRepository;

    @Transactional
    public MessageDto sendMessage(Long senderId, UserRole senderRole, SendMessageDto dto) {
        log.info("Sending message from user {} (role: {}) to user {} for case {}",
                senderId, senderRole, dto.getReceiverId(), dto.getCaseId());

        // Get or create conversation - THIS IS THE KEY FIX
        Conversation conversation = getOrCreateConversation(
                dto.getCaseId(),
                senderId,
                dto.getReceiverId(),
                senderRole,
                dto.getPatientName(),
                dto.getDoctorName()
        );

        UserRole receiverRole = senderRole == UserRole.PATIENT ? UserRole.DOCTOR : UserRole.PATIENT;

        Message message = Message.builder()
                .conversationId(conversation.getId())
                .senderId(senderId)
                .receiverId(dto.getReceiverId())
                .senderRole(senderRole)
                .receiverRole(receiverRole)
                .caseId(dto.getCaseId())
                .content(dto.getContent())
                .messageType(dto.getMessageType())
                .status(MessageStatus.SENT)
                .isRead(false)
                .replyToMessageId(dto.getReplyToMessageId())
                .senderName(senderRole == UserRole.PATIENT ? conversation.getPatientName() : conversation.getDoctorName())
                .receiverName(receiverRole == UserRole.PATIENT ? conversation.getPatientName() : conversation.getDoctorName())
                .build();

        message = messageRepository.save(message);

        updateConversationAfterMessage(conversation, message, senderRole);

        if (dto.getAttachmentIds() != null && !dto.getAttachmentIds().isEmpty()) {
            updateMessageAttachments(message.getId(), dto.getAttachmentIds());
        }

        return mapToMessageDto(message);
    }

    public List<MessageDto> getConversationMessages(Long conversationId, Long userId, int page, int size) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException("Conversation not found", HttpStatus.NOT_FOUND));

        if (!conversation.getPatientId().equals(userId) && !conversation.getDoctorId().equals(userId)) {
            throw new BusinessException("Access denied", HttpStatus.FORBIDDEN);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messagesPage = messageRepository
                .findByConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(conversationId, pageable);

        return messagesPage.getContent().stream()
                .map(this::mapToMessageDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markMessageAsRead(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException("Message not found", HttpStatus.NOT_FOUND));

        if (!message.getReceiverId().equals(userId)) {
            throw new BusinessException("Not authorized", HttpStatus.FORBIDDEN);
        }

        if (!message.getIsRead()) {
            message.setIsRead(true);
            message.setReadAt(LocalDateTime.now());
            message.setStatus(MessageStatus.READ);
            messageRepository.save(message);

            updateConversationUnreadCount(message.getConversationId(), userId);
        }
    }

    @Transactional
    public void markConversationAsRead(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException("Conversation not found", HttpStatus.NOT_FOUND));

        if (!conversation.getPatientId().equals(userId) && !conversation.getDoctorId().equals(userId)) {
            throw new BusinessException("Access denied", HttpStatus.FORBIDDEN);
        }

        messageRepository.markConversationMessagesAsRead(
                conversationId,
                userId,
                LocalDateTime.now(),
                MessageStatus.READ
        );

        if (conversation.getPatientId().equals(userId)) {
            conversation.setUnreadCountPatient(0);
        } else {
            conversation.setUnreadCountDoctor(0);
        }
        conversationRepository.save(conversation);
    }

    public UnreadCountDto getUnreadCount(Long userId, UserRole userRole) {
        Long totalUnread = messageRepository.countByReceiverIdAndIsReadFalseAndIsDeletedFalse(userId);

        List<Conversation> conversations = userRole == UserRole.PATIENT
                ? conversationRepository.findByPatientIdAndIsDeletedFalseOrderByLastMessageAtDesc(
                userId, PageRequest.of(0, 100)).getContent()
                : conversationRepository.findByDoctorIdAndIsDeletedFalseOrderByLastMessageAtDesc(
                userId, PageRequest.of(0, 100)).getContent();

        List<ConversationUnreadDto> conversationUnreads = conversations.stream()
                .map(conv -> {
                    Integer unreadCount = userRole == UserRole.PATIENT
                            ? conv.getUnreadCountPatient()
                            : conv.getUnreadCountDoctor();

                    return ConversationUnreadDto.builder()
                            .conversationId(conv.getId())
                            .caseId(conv.getCaseId())
                            .unreadCount(unreadCount)
                            .build();
                })
                .filter(dto -> dto.getUnreadCount() > 0)
                .collect(Collectors.toList());

        return UnreadCountDto.builder()
                .totalUnread(totalUnread)
                .conversationUnreads(conversationUnreads)
                .build();
    }

    public List<MessageDto> searchMessages(Long conversationId, String query, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException("Conversation not found", HttpStatus.NOT_FOUND));

        if (!conversation.getPatientId().equals(userId) && !conversation.getDoctorId().equals(userId)) {
            throw new BusinessException("Access denied", HttpStatus.FORBIDDEN);
        }

        List<Message> messages = messageRepository.searchInConversation(conversationId, query);

        return messages.stream()
                .map(this::mapToMessageDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException("Message not found", HttpStatus.NOT_FOUND));

        if (!message.getSenderId().equals(userId)) {
            throw new BusinessException("Can only delete own messages", HttpStatus.FORBIDDEN);
        }

        message.setIsDeleted(true);
        message.setDeletedAt(LocalDateTime.now());
        message.setStatus(MessageStatus.DELETED);
        messageRepository.save(message);
    }

    // ============================================
    // Private Helper Methods - KEY FIX HERE
    // ============================================

    /**
     * Get existing conversation or create new one
     * This is the key method that fixes the issue
     */
    private Conversation getOrCreateConversation(Long caseId, Long senderId, Long receiverId,
                                                 UserRole senderRole, String patientName, String doctorName) {
        // Try to find existing conversation
        return conversationRepository.findByCaseIdAndIsDeletedFalse(caseId)
                .orElseGet(() -> {
                    log.info("Creating new conversation for case {}", caseId);

                    // Determine patient and doctor IDs based on sender role
                    Long patientId = senderRole == UserRole.PATIENT ? senderId : receiverId;
                    Long doctorId = senderRole == UserRole.DOCTOR ? senderId : receiverId;

                    // Create new conversation
                    Conversation newConversation = Conversation.builder()
                            .caseId(caseId)
                            .patientId(patientId)
                            .doctorId(doctorId)
                            .title("Case #" + caseId + " Discussion")
                            .status(ConversationStatus.ACTIVE)
                            .patientName(patientName)
                            .doctorName(doctorName)
                            .totalMessagesCount(0)
                            .unreadCountPatient(0)
                            .unreadCountDoctor(0)
                            .build();

                    Conversation saved = conversationRepository.save(newConversation);
                    log.info("Created conversation {} for case {}", saved.getId(), caseId);

                    return saved;
                });
    }

    private void updateConversationAfterMessage(Conversation conversation, Message message, UserRole senderRole) {
        conversation.setLastMessageId(message.getId());
        conversation.setLastMessageAt(message.getCreatedAt());
        conversation.setLastMessagePreview(
                message.getContent().length() > 100
                        ? message.getContent().substring(0, 97) + "..."
                        : message.getContent()
        );

        if (senderRole == UserRole.PATIENT) {
            conversation.setUnreadCountDoctor(conversation.getUnreadCountDoctor() + 1);
        } else {
            conversation.setUnreadCountPatient(conversation.getUnreadCountPatient() + 1);
        }

        conversation.setTotalMessagesCount(conversation.getTotalMessagesCount() + 1);
        conversationRepository.save(conversation);
    }

    private void updateConversationUnreadCount(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException("Conversation not found", HttpStatus.NOT_FOUND));

        if (conversation.getPatientId().equals(userId)) {
            conversation.setUnreadCountPatient(Math.max(0, conversation.getUnreadCountPatient() - 1));
        } else {
            conversation.setUnreadCountDoctor(Math.max(0, conversation.getUnreadCountDoctor() - 1));
        }

        conversationRepository.save(conversation);
    }

    private void updateMessageAttachments(Long messageId, List<Long> attachmentIds) {
        attachmentIds.forEach(attachmentId -> {
            attachmentRepository.findById(attachmentId).ifPresent(attachment -> {
                attachment.setMessageId(messageId);
                attachmentRepository.save(attachment);
            });
        });
    }

    private MessageDto mapToMessageDto(Message message) {
        ModelMapper modelMapper = new ModelMapper();
        MessageDto dto = modelMapper.map(message, MessageDto.class);

        List<MessageAttachment> attachments = attachmentRepository
                .findByMessageIdAndIsDeletedFalse(message.getId());

        dto.setAttachments(attachments.stream()
                .map(att -> modelMapper.map(att, MessageAttachmentDto.class))
                .collect(Collectors.toList()));

        return dto;
    }
}