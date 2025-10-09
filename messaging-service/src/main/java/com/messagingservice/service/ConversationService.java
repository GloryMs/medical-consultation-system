package com.messagingservice.service;

import com.commonlibrary.entity.UserRole;
import com.commonlibrary.exception.BusinessException;
import com.messagingservice.dto.ConversationDto;
import com.messagingservice.dto.CreateConversationDto;
import com.messagingservice.entity.Conversation;
import com.messagingservice.entity.ConversationStatus;
import com.messagingservice.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository conversationRepository;

    @Transactional
    public ConversationDto createConversation(CreateConversationDto dto) {
        log.info("Creating conversation for case {}", dto.getCaseId());

        if (conversationRepository.existsByCaseIdAndIsDeletedFalse(dto.getCaseId())) {
            throw new BusinessException("Conversation already exists for this case", HttpStatus.CONFLICT);
        }

        Conversation conversation = Conversation.builder()
                .patientId(dto.getPatientId())
                .doctorId(dto.getDoctorId())
                .caseId(dto.getCaseId())
                .title(dto.getTitle())
                .status(ConversationStatus.ACTIVE)
                .patientName(dto.getPatientName())
                .doctorName(dto.getDoctorName())
                .totalMessagesCount(0)
                .unreadCountPatient(0)
                .unreadCountDoctor(0)
                .build();

        conversation = conversationRepository.save(conversation);
        return mapToDto(conversation, null, null);
    }

    public ConversationDto getConversationById(Long conversationId, Long userId, UserRole userRole) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException("Conversation not found", HttpStatus.NOT_FOUND));

        if (!conversation.getPatientId().equals(userId) && !conversation.getDoctorId().equals(userId)) {
            throw new BusinessException("Access denied", HttpStatus.FORBIDDEN);
        }

        return mapToDto(conversation, userId, userRole);
    }

    public ConversationDto getConversationByCase(Long caseId, Long userId, UserRole userRole) {
        Conversation conversation = conversationRepository.findByCaseIdAndIsDeletedFalse(caseId)
                .orElseThrow(() -> new BusinessException("Conversation not found for this case", HttpStatus.NOT_FOUND));

        if (!conversation.getPatientId().equals(userId) && !conversation.getDoctorId().equals(userId)) {
            throw new BusinessException("Access denied", HttpStatus.FORBIDDEN);
        }

        return mapToDto(conversation, userId, userRole);
    }

    public List<ConversationDto> getUserConversations(
            Long userId,
            UserRole userRole,
            ConversationStatus status,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Conversation> conversationsPage;

        if (userRole == UserRole.PATIENT) {
            if (status != null) {
                List<Conversation> conversations = conversationRepository
                        .findByPatientIdAndStatusAndIsDeletedFalseOrderByLastMessageAtDesc(userId, status);
                return conversations.stream()
                        .map(conv -> mapToDto(conv, userId, userRole))
                        .collect(Collectors.toList());
            } else {
                conversationsPage = conversationRepository
                        .findByPatientIdAndIsDeletedFalseOrderByLastMessageAtDesc(userId, pageable);
            }
        } else {
            if (status != null) {
                List<Conversation> conversations = conversationRepository
                        .findByDoctorIdAndStatusAndIsDeletedFalseOrderByLastMessageAtDesc(userId, status);
                return conversations.stream()
                        .map(conv -> mapToDto(conv, userId, userRole))
                        .collect(Collectors.toList());
            } else {
                conversationsPage = conversationRepository
                        .findByDoctorIdAndIsDeletedFalseOrderByLastMessageAtDesc(userId, pageable);
            }
        }

        return conversationsPage.getContent().stream()
                .map(conv -> mapToDto(conv, userId, userRole))
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateConversationStatus(Long conversationId, Long userId, ConversationStatus status) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException("Conversation not found", HttpStatus.NOT_FOUND));

        if (!conversation.getPatientId().equals(userId) && !conversation.getDoctorId().equals(userId)) {
            throw new BusinessException("Access denied", HttpStatus.FORBIDDEN);
        }

        conversation.setStatus(status);
        conversationRepository.save(conversation);
    }

    @Transactional
    public void archiveConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException("Conversation not found", HttpStatus.NOT_FOUND));

        if (!conversation.getPatientId().equals(userId) && !conversation.getDoctorId().equals(userId)) {
            throw new BusinessException("Access denied", HttpStatus.FORBIDDEN);
        }

        conversation.setIsArchived(true);
        conversation.setStatus(ConversationStatus.ARCHIVED);
        conversationRepository.save(conversation);
    }

    public List<ConversationDto> searchConversations(Long userId, UserRole userRole, String query) {
        List<Conversation> conversations = conversationRepository.searchConversations(userId, query);

        return conversations.stream()
                .map(conv -> mapToDto(conv, userId, userRole))
                .collect(Collectors.toList());
    }

    // ============================================
    // FIXED: Manual mapping instead of ModelMapper
    // ============================================
    private ConversationDto mapToDto(Conversation conversation, Long currentUserId, UserRole userRole) {
        ConversationDto dto = ConversationDto.builder()
                .id(conversation.getId())
                .patientId(conversation.getPatientId())
                .doctorId(conversation.getDoctorId())
                .caseId(conversation.getCaseId())
                .title(conversation.getTitle())
                .status(conversation.getStatus() != null ? conversation.getStatus().name() : null)
                .lastMessageId(conversation.getLastMessageId())
                .lastMessageAt(conversation.getLastMessageAt())
                .lastMessagePreview(conversation.getLastMessagePreview())
                .totalMessagesCount(conversation.getTotalMessagesCount())
                .patientName(conversation.getPatientName())
                .doctorName(conversation.getDoctorName())
                .createdAt(conversation.getCreatedAt())
                .build();

        // Set user-specific fields
        if (currentUserId != null && userRole != null) {
            if (userRole == UserRole.PATIENT) {
                dto.setUnreadCount(conversation.getUnreadCountPatient());
                dto.setOtherUserId(conversation.getDoctorId());
                dto.setOtherUserName(conversation.getDoctorName());
            } else {
                dto.setUnreadCount(conversation.getUnreadCountDoctor());
                dto.setOtherUserId(conversation.getPatientId());
                dto.setOtherUserName(conversation.getPatientName());
            }
        } else {
            // Default values when user context is not available
            dto.setUnreadCount(0);
            dto.setOtherUserId(null);
            dto.setOtherUserName(null);
        }

        // TODO: Set online status from cache/session
        dto.setIsOnline(false);

        return dto;
    }
}