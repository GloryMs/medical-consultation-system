package com.messagingservice.repository;

import com.messagingservice.entity.Conversation;
import com.messagingservice.entity.ConversationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
    Optional<Conversation> findByCaseIdAndIsDeletedFalse(Long caseId);

    Page<Conversation> findByPatientIdAndIsDeletedFalseOrderByLastMessageAtDesc(
        Long patientId, Pageable pageable
    );

    List<Conversation> findByPatientIdAndStatusAndIsDeletedFalseOrderByLastMessageAtDesc(
        Long patientId, ConversationStatus status
    );

    Page<Conversation> findByDoctorIdAndIsDeletedFalseOrderByLastMessageAtDesc(
        Long doctorId, Pageable pageable
    );

    List<Conversation> findByDoctorIdAndStatusAndIsDeletedFalseOrderByLastMessageAtDesc(
        Long doctorId, ConversationStatus status
    );

    boolean existsByCaseIdAndIsDeletedFalse(Long caseId);

    @Query("SELECT c FROM Conversation c WHERE " +
           "(c.patientId = :userId OR c.doctorId = :userId) " +
           "AND c.isDeleted = false " +
           "AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.lastMessagePreview) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Conversation> searchConversations(
        @Param("userId") Long userId,
        @Param("query") String query
    );

    Long countByPatientIdAndStatusAndIsDeletedFalse(
        Long patientId, ConversationStatus status
    );

    Long countByDoctorIdAndStatusAndIsDeletedFalse(
        Long doctorId, ConversationStatus status
    );

    // ============================================
    // Supervisor-specific queries
    // ============================================

    /**
     * Find conversations by list of patient IDs (for supervisors)
     */
    Page<Conversation> findByPatientIdInAndIsDeletedFalseOrderByLastMessageAtDesc(
        List<Long> patientIds, Pageable pageable
    );

    /**
     * Find conversations by list of patient IDs with status filter (for supervisors)
     */
    List<Conversation> findByPatientIdInAndStatusAndIsDeletedFalseOrderByLastMessageAtDesc(
        List<Long> patientIds, ConversationStatus status
    );

    /**
     * Search conversations by patient IDs (for supervisors)
     */
    @Query("SELECT c FROM Conversation c WHERE " +
           "c.patientId IN :patientIds " +
           "AND c.isDeleted = false " +
           "AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.lastMessagePreview) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Conversation> searchConversationsByPatientIds(
        @Param("patientIds") List<Long> patientIds,
        @Param("query") String query
    );
}