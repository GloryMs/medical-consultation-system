package com.messagingservice.repository;

import com.messagingservice.entity.Message;
import com.messagingservice.entity.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    Page<Message> findByConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(
        Long conversationId, Pageable pageable
    );

    List<Message> findByConversationIdAndIsDeletedFalseOrderByCreatedAtAsc(
        Long conversationId
    );

    List<Message> findByReceiverIdAndIsReadFalseAndIsDeletedFalse(Long receiverId);

    Long countByReceiverIdAndIsReadFalseAndIsDeletedFalse(Long receiverId);

    Long countByConversationIdAndReceiverIdAndIsReadFalseAndIsDeletedFalse(
        Long conversationId, Long receiverId
    );

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true, m.readAt = :readAt, m.status = :status " +
           "WHERE m.conversationId = :conversationId AND m.receiverId = :receiverId AND m.isRead = false")
    void markConversationMessagesAsRead(
        @Param("conversationId") Long conversationId,
        @Param("receiverId") Long receiverId,
        @Param("readAt") LocalDateTime readAt,
        @Param("status") MessageStatus status
    );

    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId " +
           "AND m.isDeleted = false " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Message> searchInConversation(
        @Param("conversationId") Long conversationId,
        @Param("query") String query
    );

    Message findTopByConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(Long conversationId);

    List<Message> findByCaseIdAndIsDeletedFalseOrderByCreatedAtAsc(Long caseId);

    Long countByConversationIdAndIsDeletedFalse(Long conversationId);
}