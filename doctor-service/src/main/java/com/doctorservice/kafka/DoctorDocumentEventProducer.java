package com.doctorservice.kafka;

import com.commonlibrary.dto.NotificationDto;
import com.commonlibrary.entity.NotificationPriority;
import com.commonlibrary.entity.NotificationType;
import com.commonlibrary.entity.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka event producer for doctor document-related events
 * Sends notifications to admins when doctors upload/submit documents
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DoctorDocumentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Send notification to admin when doctor uploads a new document
     * This helps admin track document submissions in real-time
     */
    public void sendDocumentUploadedNotification(Long doctorId, Long doctorUserId, String doctorName, 
                                                 String doctorEmail, String documentType, 
                                                 String fileName, Long documentId) {
        try {
            // Send event to admin
            Map<String, Object> documentEvent = new HashMap<>();
            documentEvent.put("eventType", "DOCTOR_DOCUMENT_UPLOADED");
            documentEvent.put("doctorId", doctorId);
            documentEvent.put("doctorUserId", doctorUserId);
            documentEvent.put("doctorName", doctorName);
            documentEvent.put("doctorEmail", doctorEmail);
            documentEvent.put("documentType", documentType);
            documentEvent.put("fileName", fileName);
            documentEvent.put("documentId", documentId);
            documentEvent.put("timestamp", System.currentTimeMillis());

            kafkaTemplate.send("doctor-document-uploaded-topic", documentEvent);
            log.info("Document upload event sent for doctor {} - Type: {}", doctorId, documentType);

            // Send notification to admin (assuming admin userId is 1 - adjust as needed)
            NotificationDto adminNotification = NotificationDto.builder()
                    .senderUserId(0L)
                    .receiverUserId(1L)
                    .senderType(UserType.SYSTEM)
                    .receiverType(UserType.ADMIN)
                    .title("New Doctor Document Uploaded")
                    .message(String.format(
                        "Dr. %s has uploaded a new document:\n\n" +
                        "Document Type: %s\n" +
                        "File Name: %s\n" +
                        "Email: %s\n\n" +
                        "Please review the document in the verification panel.",
                        doctorName,
                        documentType,
                        fileName,
                        doctorEmail
                    ))
                    .type(NotificationType.DOCTOR_DOCUMENT)
                    .sendEmail(false) // Don't spam admin email for every upload
                    .priority(NotificationPriority.MEDIUM)
                    .build();

            kafkaTemplate.send("notification-topic", adminNotification);
            log.info("Admin notification sent for document upload by doctor: {}", doctorEmail);

        } catch (Exception e) {
            log.error("Error sending document upload notification for doctor {}: {}", doctorId, e.getMessage(), e);
        }
    }

    /**
     * Send notification to admin when doctor submits all documents for review
     * This triggers admin to start the verification process
     */
    public void sendDocumentsSubmittedForReviewNotification(Long doctorId, Long doctorUserId, 
                                                            String doctorName, String doctorEmail,
                                                            int totalDocuments) {
        try {
            // Send event
            Map<String, Object> submitEvent = new HashMap<>();
            submitEvent.put("eventType", "DOCTOR_DOCUMENTS_SUBMITTED");
            submitEvent.put("doctorId", doctorId);
            submitEvent.put("doctorUserId", doctorUserId);
            submitEvent.put("doctorName", doctorName);
            submitEvent.put("doctorEmail", doctorEmail);
            submitEvent.put("totalDocuments", totalDocuments);
            submitEvent.put("timestamp", System.currentTimeMillis());

            kafkaTemplate.send("doctor-documents-submitted-topic", submitEvent);
            log.info("Documents submission event sent for doctor {} - {} documents", doctorId, totalDocuments);

            // Send HIGH priority notification to admin
            NotificationDto adminNotification = NotificationDto.builder()
                    .senderUserId(0L)
                    .receiverUserId(1L)
                    .senderType(UserType.SYSTEM)
                    .receiverType(UserType.ADMIN)
                    .title("Doctor Documents Ready for Verification")
                    .message(String.format(
                        "Dr. %s has submitted %d documents for verification:\n\n" +
                        "Email: %s\n" +
                        "Status: Pending Verification\n\n" +
                        "Action Required: Please review and verify the submitted documents.",
                        doctorName,
                        totalDocuments,
                        doctorEmail
                    ))
                    .type(NotificationType.DOCTOR_VERIFICATION)
                    .sendEmail(true) // Send email for submission
                    .priority(NotificationPriority.HIGH)
                    .build();

            kafkaTemplate.send("notification-topic", adminNotification);
            log.info("Admin notification sent for document submission by doctor: {}", doctorEmail);

        } catch (Exception e) {
            log.error("Error sending documents submission notification for doctor {}: {}", doctorId, e.getMessage(), e);
        }
    }

    /**
     * Send notification to admin when doctor re-uploads a rejected document
     * This is for tracking corrections after rejection
     */
    public void sendDocumentReuploadedNotification(Long doctorId, Long doctorUserId, String doctorName,
                                                   String doctorEmail, String documentType,
                                                   String fileName, Long documentId) {
        try {
            // Send event
            Map<String, Object> reuploadEvent = new HashMap<>();
            reuploadEvent.put("eventType", "DOCTOR_DOCUMENT_REUPLOADED");
            reuploadEvent.put("doctorId", doctorId);
            reuploadEvent.put("doctorUserId", doctorUserId);
            reuploadEvent.put("doctorName", doctorName);
            reuploadEvent.put("documentType", documentType);
            reuploadEvent.put("fileName", fileName);
            reuploadEvent.put("documentId", documentId);
            reuploadEvent.put("timestamp", System.currentTimeMillis());

            kafkaTemplate.send("doctor-document-corrected-topic", reuploadEvent);
            log.info("Document re-upload event sent for doctor {} - Type: {}", doctorId, documentType);

            // Send notification to admin
            NotificationDto adminNotification = NotificationDto.builder()
                    .senderUserId(0L)
                    .receiverUserId(1L)
                    .senderType(UserType.SYSTEM)
                    .receiverType(UserType.ADMIN)
                    .title("Doctor Re-uploaded Document")
                    .message(String.format(
                        "Dr. %s has re-uploaded a corrected document:\n\n" +
                        "Document Type: %s\n" +
                        "File Name: %s\n" +
                        "Email: %s\n\n" +
                        "This document was previously rejected. Please review the updated version.",
                        doctorName,
                        documentType,
                        fileName,
                        doctorEmail
                    ))
                    .type(NotificationType.DOCTOR_DOCUMENT)
                    .sendEmail(true) // Send email for corrections
                    .priority(NotificationPriority.HIGH)
                    .build();

            kafkaTemplate.send("notification-topic", adminNotification);
            log.info("Admin notification sent for document re-upload by doctor: {}", doctorEmail);

        } catch (Exception e) {
            log.error("Error sending document re-upload notification for doctor {}: {}", doctorId, e.getMessage(), e);
        }
    }

    /**
     * Send notification to doctor when their document is verified by admin
     */
    public void sendDocumentVerifiedNotification(Long doctorId, Long doctorUserId, String doctorName,
                                                String doctorEmail, String documentType,
                                                String verificationNotes) {
        try {
            NotificationDto doctorNotification = NotificationDto.builder()
                    .senderUserId(1L)
                    .receiverUserId(doctorUserId)
                    .senderType(UserType.ADMIN)
                    .receiverType(UserType.DOCTOR)
                    .title("Document Verified Successfully")
                    .message(String.format(
                        "Hello Dr. %s,\n\n" +
                        "Great news! Your %s document has been verified by our admin team.\n\n" +
                        "%s\n\n" +
                        "You are one step closer to activating your account.",
                        doctorName,
                        documentType,
                        verificationNotes != null && !verificationNotes.isEmpty() 
                            ? "Admin Notes: " + verificationNotes 
                            : ""
                    ))
                    .type(NotificationType.DOCTOR_DOCUMENT)
                    .sendEmail(true)
                    .recipientEmail(doctorEmail)
                    .priority(NotificationPriority.MEDIUM)
                    .build();

            kafkaTemplate.send("notification-topic", doctorNotification);
            log.info("Document verification success notification sent to doctor: {}", doctorEmail);

        } catch (Exception e) {
            log.error("Error sending document verification notification to doctor {}: {}", doctorId, e.getMessage(), e);
        }
    }

    /**
     * Send notification to doctor when their document is rejected by admin
     */
    public void sendDocumentRejectedNotification(Long doctorId, Long doctorUserId, String doctorName,
                                                String doctorEmail, String documentType,
                                                String verificationNotes) {
        try {
            NotificationDto doctorNotification = NotificationDto.builder()
                    .senderUserId(1L)
                    .receiverUserId(doctorUserId)
                    .senderType(UserType.ADMIN)
                    .receiverType(UserType.DOCTOR)
                    .title("Document Verification Issue")
                    .message(String.format(
                        "Hello Dr. %s,\n\n" +
                        "Your %s document requires attention.\n\n" +
                        "Admin Notes: %s\n\n" +
                        "Please upload a corrected version of the document to proceed with your verification.",
                        doctorName,
                        documentType,
                        verificationNotes != null && !verificationNotes.isEmpty() 
                            ? verificationNotes 
                            : "Document does not meet verification requirements."
                    ))
                    .type(NotificationType.DOCTOR_DOCUMENT)
                    .sendEmail(true)
                    .recipientEmail(doctorEmail)
                    .priority(NotificationPriority.HIGH)
                    .build();

            kafkaTemplate.send("notification-topic", doctorNotification);
            log.info("Document rejection notification sent to doctor: {}", doctorEmail);

        } catch (Exception e) {
            log.error("Error sending document rejection notification to doctor {}: {}", doctorId, e.getMessage(), e);
        }
    }

    /**
     * Send notification when all doctor documents are verified
     * This indicates doctor is ready for final account approval
     */
    public void sendAllDocumentsVerifiedNotification(Long doctorId, Long doctorUserId, String doctorName,
                                                     String doctorEmail, int totalDocuments) {
        try {
            // Notify doctor
            NotificationDto doctorNotification = NotificationDto.builder()
                    .senderUserId(1L)
                    .receiverUserId(doctorUserId)
                    .senderType(UserType.ADMIN)
                    .receiverType(UserType.DOCTOR)
                    .title("All Documents Verified - Account Pending Final Approval")
                    .message(String.format(
                        "Congratulations Dr. %s!\n\n" +
                        "All %d of your submitted documents have been verified.\n\n" +
                        "Your account is now pending final approval. You will receive a notification " +
                        "once your account is fully activated.\n\n" +
                        "Thank you for your patience!",
                        doctorName,
                        totalDocuments
                    ))
                    .type(NotificationType.DOCTOR_VERIFICATION)
                    .sendEmail(true)
                    .recipientEmail(doctorEmail)
                    .priority(NotificationPriority.HIGH)
                    .build();

            kafkaTemplate.send("notification-topic", doctorNotification);
            log.info("All documents verified notification sent to doctor: {}", doctorEmail);

            // Notify admin that doctor is ready for final approval
            NotificationDto adminNotification = NotificationDto.builder()
                    .senderUserId(doctorUserId)
                    .receiverUserId(1L)
                    .senderType(UserType.DOCTOR)
                    .receiverType(UserType.ADMIN)
                    .title("Doctor Ready for Final Verification")
                    .message(String.format(
                        "Dr. %s has all documents verified and is ready for final account approval:\n\n" +
                        "Email: %s\n" +
                        "Documents Verified: %d\n\n" +
                        "Action Required: Please complete the final doctor verification.",
                        doctorName,
                        doctorEmail,
                        totalDocuments
                    ))
                    .type(NotificationType.DOCTOR_VERIFICATION)
                    .sendEmail(true)
                    .priority(NotificationPriority.HIGH)
                    .build();

            kafkaTemplate.send("notification-topic", adminNotification);
            log.info("Admin notification sent - Doctor ready for final approval: {}", doctorEmail);

        } catch (Exception e) {
            log.error("Error sending all documents verified notification for doctor {}: {}", doctorId, e.getMessage(), e);
        }
    }
}