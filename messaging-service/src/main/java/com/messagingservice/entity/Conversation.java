package com.messagingservice.entity;

import com.commonlibrary.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversations", indexes = {
    @Index(name = "idx_patient_doctor", columnList = "patientId, doctorId"),
    @Index(name = "idx_case_id", columnList = "caseId"),
    @Index(name = "idx_last_message", columnList = "lastMessageAt")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation extends BaseEntity {

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private Long doctorId;

    @Column(nullable = false, unique = true)
    private Long caseId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ConversationStatus status = ConversationStatus.ACTIVE;

    private Long lastMessageId;
    private LocalDateTime lastMessageAt;

    @Column(columnDefinition = "TEXT")
    private String lastMessagePreview;

    @Column(nullable = false)
    @Builder.Default
    private Integer unreadCountPatient = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer unreadCountDoctor = 0;

    @Builder.Default
    private Integer totalMessagesCount = 0;

    private String patientName;
    private String doctorName;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isArchived = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    private LocalDateTime archivedAt;
}