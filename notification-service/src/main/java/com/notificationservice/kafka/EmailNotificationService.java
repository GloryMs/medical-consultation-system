package com.notificationservice.kafka;

import com.commonlibrary.dto.EmailNotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;

/**
 * Service for handling email notifications
 * Listens to Kafka topic and sends emails via SMTP
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.mail.sender-name:Medical Consultation System}")
    private String defaultSenderName;

    /**
     * Kafka listener for email notifications
     * Consumes messages from 'email-notifications' topic
     */
    @KafkaListener(topics = "email-notifications", groupId = "notification-group")
    public void consumeEmailNotification(EmailNotificationDto notification) {
        try {
            log.info("Received email notification for: {}", notification.getRecipient());
            
            sendEmail(notification);
            
            log.info("Email sent successfully to: {}", notification.getRecipient());
        } catch (Exception e) {
            log.error("Error sending email to: {}", notification.getRecipient(), e);
            // TODO: Implement retry logic or dead letter queue
            handleEmailFailure(notification, e);
        }
    }

    /**
     * Send email using JavaMailSender
     */
    private void sendEmail(EmailNotificationDto notification) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        // Set sender
        String senderName = notification.getSenderName() != null 
            ? notification.getSenderName() 
            : defaultSenderName;
        helper.setFrom(fromEmail, senderName);

        // Set recipient
        helper.setTo(notification.getRecipient());

        // Set subject
        helper.setSubject(notification.getSubject());

        // Set body (HTML or plain text)
        boolean isHtml = notification.getIsHtml() != null ? notification.getIsHtml() : true;
        helper.setText(notification.getBody(), isHtml);

        // Set CC if present
        if (notification.getCc() != null && !notification.getCc().isEmpty()) {
            helper.setCc(notification.getCc().toArray(new String[0]));
        }

        // Set BCC if present
        if (notification.getBcc() != null && !notification.getBcc().isEmpty()) {
            helper.setBcc(notification.getBcc().toArray(new String[0]));
        }

        // Set Reply-To if present
        if (notification.getReplyTo() != null && !notification.getReplyTo().isEmpty()) {
            helper.setReplyTo(notification.getReplyTo());
        }

        // Set priority if present
        if (notification.getPriority() != null) {
            switch (notification.getPriority().toUpperCase()) {
                case "HIGH":
                    message.setHeader("X-Priority", "1");
                    message.setHeader("Importance", "high");
                    break;
                case "LOW":
                    message.setHeader("X-Priority", "5");
                    message.setHeader("Importance", "low");
                    break;
                default:
                    message.setHeader("X-Priority", "3");
                    message.setHeader("Importance", "normal");
            }
        }

        // Add attachments if present
        // TODO: Implement attachment handling if needed
        if (notification.getAttachments() != null && !notification.getAttachments().isEmpty()) {
            log.warn("Attachments not yet implemented for email: {}", notification.getRecipient());
        }

        // Send the email
        try {
            mailSender.send(message);
            log.debug("Email sent successfully - Type: {}, Recipient: {}", 
                     notification.getEmailType(), notification.getRecipient());
        } catch (MailException e) {
            log.error("Failed to send email to: {}", notification.getRecipient(), e);
            throw e;
        }
    }

    /**
     * Handle email sending failure
     * Log the error and optionally implement retry logic
     */
    private void handleEmailFailure(EmailNotificationDto notification, Exception e) {
        log.error("Email delivery failed for recipient: {}, Type: {}, Error: {}", 
                 notification.getRecipient(), 
                 notification.getEmailType(), 
                 e.getMessage());
        
        // TODO: Implement the following:
        // 1. Store failed emails in database for retry
        // 2. Send to dead letter queue if max retries exceeded
        // 3. Send alert to admin if critical email fails
        // 4. Implement exponential backoff for retries
        
        // Example retry logic (implement based on your requirements):
        // if (shouldRetry(notification)) {
        //     retryEmailSending(notification);
        // } else {
        //     sendToDeadLetterQueue(notification);
        // }
    }

    /**
     * Send a simple email (convenience method)
     * Can be called directly from other services
     */
    public void sendSimpleEmail(String to, String subject, String body) {
        EmailNotificationDto notification = EmailNotificationDto.builder()
            .recipient(to)
            .subject(subject)
            .body(body)
            .isHtml(false)
            .build();
        
        try {
            sendEmail(notification);
            log.info("Simple email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send simple email to: {}", to, e);
        }
    }

    /**
     * Send HTML email (convenience method)
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        EmailNotificationDto notification = EmailNotificationDto.builder()
            .recipient(to)
            .subject(subject)
            .body(htmlBody)
            .isHtml(true)
            .build();
        
        try {
            sendEmail(notification);
            log.info("HTML email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send HTML email to: {}", to, e);
        }
    }

    /**
     * Send email with CC and BCC
     */
    public void sendEmailWithCopies(String to, String subject, String body, 
                                    String[] cc, String[] bcc) {
        EmailNotificationDto notification = EmailNotificationDto.builder()
            .recipient(to)
            .subject(subject)
            .body(body)
            .cc(cc != null ? java.util.Arrays.asList(cc) : null)
            .bcc(bcc != null ? java.util.Arrays.asList(bcc) : null)
            .isHtml(true)
            .build();
        
        try {
            sendEmail(notification);
            log.info("Email with copies sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email with copies to: {}", to, e);
        }
    }

    /**
     * Send high priority email
     */
    public void sendUrgentEmail(String to, String subject, String body) {
        EmailNotificationDto notification = EmailNotificationDto.builder()
            .recipient(to)
            .subject(subject)
            .body(body)
            .priority("HIGH")
            .isHtml(true)
            .build();
        
        try {
            sendEmail(notification);
            log.info("Urgent email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send urgent email to: {}", to, e);
        }
    }
}