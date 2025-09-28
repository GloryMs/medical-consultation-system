package com.doctorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardNotificationDto {
    private Long id;
    private String type;
    private String title;
    private String message;
    private LocalDateTime createdAt;
    private Boolean isRead;
    private String actionUrl;
}