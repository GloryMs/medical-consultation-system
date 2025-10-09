package com.messagingservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageAttachmentDto {
    
    private Long id;
    private Long messageId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private String attachmentType;
    private String thumbnailUrl;
    private LocalDateTime createdAt;
}