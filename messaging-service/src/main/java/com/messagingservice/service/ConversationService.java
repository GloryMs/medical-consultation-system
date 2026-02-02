package com.messagingservice.service;

import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.entity.UserRole;
import com.commonlibrary.exception.BusinessException;
import com.messagingservice.dto.ConversationDto;
import com.messagingservice.dto.CreateConversationDto;
import com.messagingservice.entity.Conversation;
import com.messagingservice.entity.ConversationStatus;
import com.messagingservice.feign.SupervisorServiceClient;
import com.messagingservice.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final SupervisorServiceClient supervisorServiceClient;

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

        // Authorization check
        if (userRole == UserRole.MEDICAL_SUPERVISOR) {
            // Check if supervisor has access to this patient
            List<Long> assignedPatientIds = getAssignedPatientIds(userId);
            if (!assignedPatientIds.contains(conversation.getPatientId())) {
                throw new BusinessException("Access denied", HttpStatus.FORBIDDEN);
            }
        } else {
            // For PATIENT and DOCTOR roles
            if (!conversation.getPatientId().equals(userId) && !conversation.getDoctorId().equals(userId)) {
                throw new BusinessException("Access denied", HttpStatus.FORBIDDEN);
            }
        }

        return mapToDto(conversation, userId, userRole);
    }

    public ConversationDto getConversationByCase(Long caseId, Long userId, UserRole userRole) {
        Conversation conversation = conversationRepository.findByCaseIdAndIsDeletedFalse(caseId)
                .orElseThrow(() -> new BusinessException("Conversation not found for this case", HttpStatus.NOT_FOUND));

        // Authorization check
        if (userRole == UserRole.MEDICAL_SUPERVISOR) {
            // Check if supervisor has access to this patient
            List<Long> assignedPatientIds = getAssignedPatientIds(userId);
            if (!assignedPatientIds.contains(conversation.getPatientId())) {
                throw new BusinessException("Access denied", HttpStatus.FORBIDDEN);
            }
        } else {
            // For PATIENT and DOCTOR roles
            if (!conversation.getPatientId().equals(userId) && !conversation.getDoctorId().equals(userId)) {
                throw new BusinessException("Access denied", HttpStatus.FORBIDDEN);
            }
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
        } else if (userRole == UserRole.MEDICAL_SUPERVISOR) {
            // Fetch assigned patient IDs from supervisor service
            List<Long> patientIds = getAssignedPatientIds(userId);

            // If no assigned patients, return empty list
            if (patientIds.isEmpty()) {
                return Collections.emptyList();
            }
            log.info("Assigned patients for supervisor {} are: [{}]", userId, patientIds.toString());

            if (status != null) {
                List<Conversation> conversations = conversationRepository
                        .findByPatientIdInAndStatusAndIsDeletedFalseOrderByLastMessageAtDesc(patientIds, status);
                log.info("Conversations count for supervisor {} is: {}", userId, conversations.size());

                return conversations.stream()
                        .map(conv -> mapToDto(conv, userId, userRole))
                        .collect(Collectors.toList());
            } else {
                conversationsPage = conversationRepository
                        .findByPatientIdInAndIsDeletedFalseOrderByLastMessageAtDesc(patientIds, pageable);
                log.info("Conversations count for supervisor {} is: {}", userId, conversationsPage.
                        getContent().size());
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
    public void updateConversationStatus(Long conversationId, Long userId, ConversationStatus status, UserRole userRole) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException("Conversation not found", HttpStatus.NOT_FOUND));

        // Authorization check
        if (userRole == UserRole.MEDICAL_SUPERVISOR) {
            // Check if supervisor has access to this patient
            List<Long> assignedPatientIds = getAssignedPatientIds(userId);
            if (!assignedPatientIds.contains(conversation.getPatientId())) {
                throw new BusinessException("Access denied", HttpStatus.FORBIDDEN);
            }
        } else {
            // For PATIENT and DOCTOR roles
            if (!conversation.getPatientId().equals(userId) && !conversation.getDoctorId().equals(userId)) {
                throw new BusinessException("Access denied", HttpStatus.FORBIDDEN);
            }
        }

        conversation.setStatus(status);
        conversationRepository.save(conversation);
    }

    @Transactional
    public void archiveConversation(Long conversationId, Long userId, UserRole userRole) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException("Conversation not found", HttpStatus.NOT_FOUND));

        // Authorization check
        if (userRole == UserRole.MEDICAL_SUPERVISOR) {
            // Check if supervisor has access to this patient
            List<Long> assignedPatientIds = getAssignedPatientIds(userId);
            if (!assignedPatientIds.contains(conversation.getPatientId())) {
                throw new BusinessException("Access denied", HttpStatus.FORBIDDEN);
            }
        } else {
            // For PATIENT and DOCTOR roles
            if (!conversation.getPatientId().equals(userId) && !conversation.getDoctorId().equals(userId)) {
                throw new BusinessException("Access denied", HttpStatus.FORBIDDEN);
            }
        }

        conversation.setIsArchived(true);
        conversation.setStatus(ConversationStatus.ARCHIVED);
        conversationRepository.save(conversation);
    }

    public List<ConversationDto> searchConversations(Long userId, UserRole userRole, String query) {
        List<Conversation> conversations;

        if (userRole == UserRole.MEDICAL_SUPERVISOR) {
            // Fetch assigned patient IDs and search their conversations
            List<Long> patientIds = getAssignedPatientIds(userId);

            if (patientIds.isEmpty()) {
                return Collections.emptyList();
            }

            conversations = conversationRepository.searchConversationsByPatientIds(patientIds, query);
        } else {
            // For PATIENT and DOCTOR roles, use existing search
            conversations = conversationRepository.searchConversations(userId, query);
        }

        return conversations.stream()
                .map(conv -> mapToDto(conv, userId, userRole))
                .collect(Collectors.toList());
    }

    // ============================================
    // Helper method to fetch assigned patient IDs for supervisor
    // ============================================
    private List<Long> getAssignedPatientIds(Long supervisorId) {
        try {
            log.debug("Fetching assigned patient IDs for supervisor: {}", supervisorId);
            ResponseEntity<ApiResponse<List<Long>>> response =
                    supervisorServiceClient.getAssignedPatientIds(supervisorId);

            if (response.getBody() != null && response.getBody().getData() != null) {
                List<Long> patientIds = response.getBody().getData();
                log.debug("Found {} assigned patients for supervisor {}", patientIds.size(), supervisorId);
                return patientIds;
            }

            log.warn("No patient IDs returned for supervisor {}", supervisorId);
            return Collections.emptyList();

        } catch (Exception e) {
            log.error("Error fetching assigned patient IDs for supervisor {}: {}", supervisorId, e.getMessage());
            throw new BusinessException(
                    "Unable to fetch assigned patients. Please try again later.",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }
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
            } else if (userRole == UserRole.MEDICAL_SUPERVISOR) {
                // Supervisors see doctor's unread count (monitoring perspective)
                dto.setUnreadCount(conversation.getUnreadCountDoctor());
                dto.setOtherUserId(conversation.getPatientId());
                dto.setOtherUserName(conversation.getPatientName());
            } else {
                // DOCTOR role
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