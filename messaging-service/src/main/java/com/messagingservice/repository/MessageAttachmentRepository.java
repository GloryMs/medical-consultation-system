package com.messagingservice.repository;

import com.messagingservice.entity.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long> {
    
    List<MessageAttachment> findByMessageIdAndIsDeletedFalse(Long messageId);
    
    Long countByMessageIdAndIsDeletedFalse(Long messageId);
}