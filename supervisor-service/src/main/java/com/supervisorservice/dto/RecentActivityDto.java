package com.supervisorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for recent activity items in dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityDto {
    
    private String activityType; // CASE_SUBMITTED, APPOINTMENT_SCHEDULED, COUPON_USED, etc.
    private String description;
    private Long relatedEntityId;
    private String relatedEntityType; // CASE, APPOINTMENT, COUPON, PATIENT
    private LocalDateTime timestamp;
    private String patientName;
    private String title;
}
