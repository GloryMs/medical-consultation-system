package com.commonlibrary.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for Email Notification
 * Used to send email notifications via Kafka to notification-service
 * This class must be Serializable for Kafka message transmission
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationDto implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Email recipient address
     * Must be a valid email format
     */
    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String recipient;
    
    /**
     * Email subject line
     */
    @NotBlank(message = "Email subject is required")
    private String subject;
    
    /**
     * Email body content
     * Can be plain text or HTML
     */
    @NotBlank(message = "Email body is required")
    private String body;
    
    /**
     * CC (Carbon Copy) recipients (optional)
     */
    private List<String> cc;
    
    /**
     * BCC (Blind Carbon Copy) recipients (optional)
     */
    private List<String> bcc;
    
    /**
     * Email type/category for tracking and templates
     * Examples: VERIFICATION, PASSWORD_RESET, NOTIFICATION, APPOINTMENT, etc.
     */
    private String emailType;
    
    /**
     * Priority level (optional)
     * HIGH, NORMAL, LOW
     */
    private String priority;
    
    /**
     * Template variables for email templates (optional)
     * Key-value pairs to replace placeholders in templates
     */
    private Map<String, String> templateVariables;
    
    /**
     * Timestamp when the notification was created
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * Whether the email body contains HTML
     */
    @Builder.Default
    private Boolean isHtml = true;
    
    /**
     * Reply-to email address (optional)
     */
    private String replyTo;
    
    /**
     * Sender name to display (optional)
     * If not provided, default sender name will be used
     */
    private String senderName;
    
    /**
     * Attachments (optional)
     * List of file paths or URLs to attach
     */
    private List<String> attachments;
    
    /**
     * User ID for tracking purposes (optional)
     */
    private Long userId;
    
    /**
     * Additional metadata (optional)
     */
    private Map<String, Object> metadata;
}