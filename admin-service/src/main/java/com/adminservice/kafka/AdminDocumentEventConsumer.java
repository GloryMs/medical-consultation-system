package com.adminservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka event consumer for admin-service to handle doctor document events
 * Listens to document upload, submission, and correction events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminDocumentEventConsumer {

    /**
     * Handle doctor document uploaded event
     * This is triggered when a doctor uploads any document
     */
    @KafkaListener(topics = "doctor-document-uploaded-topic", groupId = "admin-service-group")
    public void handleDocumentUploaded(Map<String, Object> event) {
        try {
            Long doctorId = Long.valueOf(event.get("doctorId").toString());
            String doctorName = event.get("doctorName").toString();
            String doctorEmail = event.get("doctorEmail").toString();
            String documentType = event.get("documentType").toString();
            String fileName = event.get("fileName").toString();
            Long documentId = Long.valueOf(event.get("documentId").toString());
            
            log.info("Doctor document uploaded - Doctor: {} ({}), Type: {}, File: {}", 
                    doctorName, doctorEmail, documentType, fileName);
            
            // Update admin dashboard statistics
            // Could trigger real-time UI updates via WebSocket
            // Log admin audit trail
            
        } catch (Exception e) {
            log.error("Error handling document uploaded event", e);
        }
    }

    /**
     * Handle doctor documents submitted for review event
     * This is triggered when doctor submits all documents for admin verification
     */
    @KafkaListener(topics = "doctor-documents-submitted-topic", groupId = "admin-service-group")
    public void handleDocumentsSubmitted(Map<String, Object> event) {
        try {
            Long doctorId = Long.valueOf(event.get("doctorId").toString());
            String doctorName = event.get("doctorName").toString();
            String doctorEmail = event.get("doctorEmail").toString();
            int totalDocuments = Integer.parseInt(event.get("totalDocuments").toString());
            
            log.info("Doctor documents submitted for review - Doctor: {} ({}), Documents: {}", 
                    doctorName, doctorEmail, totalDocuments);
            
            // Add to admin verification queue
            // Update pending verifications count
            // Trigger admin dashboard notification
            // Could assign to specific admin for review
            
        } catch (Exception e) {
            log.error("Error handling documents submitted event", e);
        }
    }

    /**
     * Handle doctor document re-uploaded event
     * This is triggered when doctor re-uploads a document after rejection
     */
    @KafkaListener(topics = "doctor-document-corrected-topic", groupId = "admin-service-group")
    public void handleDocumentCorrected(Map<String, Object> event) {
        try {
            Long doctorId = Long.valueOf(event.get("doctorId").toString());
            String doctorName = event.get("doctorName").toString();
            String documentType = event.get("documentType").toString();
            String fileName = event.get("fileName").toString();
            Long documentId = Long.valueOf(event.get("documentId").toString());
            
            log.info("Doctor corrected document - Doctor: {}, Type: {}, File: {}", 
                    doctorName, documentType, fileName);
            
            // Update verification queue - prioritize corrected documents
            // Flag for admin review
            // Update admin audit log
            
        } catch (Exception e) {
            log.error("Error handling document corrected event", e);
        }
    }
}