package com.notificationservice.repository;

import com.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.commonlibrary.entity.UserType;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // NEW METHODS - Using userId and UserType
    List<Notification> findByReceiverUserIdAndReceiverTypeOrderByCreatedAtDesc(
            Long receiverUserId, UserType receiverType);

    List<Notification> findByReceiverUserIdAndReceiverTypeAndIsReadFalseOrderByCreatedAtDesc(
            Long receiverUserId, UserType receiverType);

    long countByReceiverUserIdAndReceiverTypeAndIsReadFalse(
            Long receiverUserId, UserType receiverType);

    // Optional: Query by sender
    List<Notification> findBySenderUserIdAndSenderTypeOrderByCreatedAtDesc(
            Long senderUserId, UserType senderType);

    @Deprecated
    List<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);
    @Deprecated
    List<Notification> findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(Long receiverId);
    @Deprecated
    long countByReceiverIdAndIsReadFalse(Long receiverId);
}
