package com.messagingservice.dto;

import com.messagingservice.entity.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageDto {
    
    @NotNull(message = "Receiver ID is required")
    private Long receiverId;
    
    @NotNull(message = "Case ID is required")
    private Long caseId;
    
    @NotBlank(message = "Message content is required")
    private String content;

    private String patientName;

    private String doctorName;
    
    private MessageType messageType = MessageType.TEXT;
    
    private Long replyToMessageId;
    
    private List<Long> attachmentIds;
}