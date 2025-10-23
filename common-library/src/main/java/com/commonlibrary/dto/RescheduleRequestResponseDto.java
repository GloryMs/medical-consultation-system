package com.commonlibrary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Response DTO for Reschedule Request
 * Used when retrieving reschedule request details from backend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleRequestResponseDto {
    
    private Long id;
    
    private Long caseId;
    
    private String requestedBy;
    
    private String reason;
    
    private String preferredTimes; // Comma-separated ISO datetime strings
    
    private String status; // PENDING, APPROVED, REJECTED
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}