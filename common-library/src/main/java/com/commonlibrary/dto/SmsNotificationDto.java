package com.commonlibrary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for SMS/WhatsApp Notification
 * Used to send SMS or WhatsApp notifications via Kafka to notification-service
 * This class must be Serializable for Kafka message transmission
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsNotificationDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Phone number in E.164 format (recommended)
     * Example: +1234567890
     * Or without + sign: 1234567890
     */
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format. Use E.164 format: +1234567890")
    private String phoneNumber;

    /**
     * SMS message content
     * Keep under 160 characters for single SMS
     * Longer messages will be split into multiple SMS
     */
    @NotBlank(message = "Message is required")
    private String message;

    /**
     * Delivery provider/method
     * Values: SMS, WHATSAPP
     */
    @Builder.Default
    private String provider = "SMS";

    /**
     * Message type/category for tracking
     * Examples: VERIFICATION, PASSWORD_RESET, APPOINTMENT_REMINDER, etc.
     */
    private String messageType;

    /**
     * Priority level (optional)
     * HIGH, NORMAL, LOW
     */
    private String priority;

    /**
     * Timestamp when the notification was created
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Country code (optional)
     * If phone number doesn't include country code
     */
    private String countryCode;

    /**
     * User ID for tracking purposes (optional)
     */
    private Long userId;

    /**
     * Schedule time for delayed sending (optional)
     * If null, send immediately
     */
    private LocalDateTime scheduledTime;

    /**
     * Maximum retry attempts if sending fails
     */
    @Builder.Default
    private Integer maxRetries = 3;

    /**
     * Additional metadata (optional)
     */
    private Map<String, Object> metadata;
}