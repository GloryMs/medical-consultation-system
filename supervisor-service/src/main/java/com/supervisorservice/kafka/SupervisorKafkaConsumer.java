package com.supervisorservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka consumer for supervisor service events from other services
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SupervisorKafkaConsumer {
    
    /**
     * Listen to case status updates from patient-service
     */
    @KafkaListener(topics = "case.status.updated", groupId = "supervisor-service-group")
    public void handleCaseStatusUpdate(Map<String, Object> event) {
        try {
            log.info("Received case status update event: {}", event);
            
            Long caseId = Long.valueOf(event.get("caseId").toString());
            String newStatus = event.get("newStatus").toString();
            Long patientId = Long.valueOf(event.get("patientId").toString());
            
            log.info("Case {} for patient {} updated to status: {}", caseId, patientId, newStatus);
            
            // TODO: Update local statistics if needed
            // TODO: Send notification to supervisor if configured
            
        } catch (Exception e) {
            log.error("Error handling case status update event", e);
        }
    }
    
    /**
     * Listen to case assignment events from patient-service
     */
    @KafkaListener(topics = "case.assigned", groupId = "supervisor-service-group")
    public void handleCaseAssigned(Map<String, Object> event) {
        try {
            log.info("Received case assigned event: {}", event);
            
            Long caseId = Long.valueOf(event.get("caseId").toString());
            Long doctorId = Long.valueOf(event.get("doctorId").toString());
            Long patientId = Long.valueOf(event.get("patientId").toString());
            
            log.info("Case {} assigned to doctor {} for patient {}", caseId, doctorId, patientId);
            
            // TODO: Send notification to supervisor
            
        } catch (Exception e) {
            log.error("Error handling case assigned event", e);
        }
    }
    
    /**
     * Listen to appointment scheduled events from doctor-service
     */
    @KafkaListener(topics = "appointment.scheduled", groupId = "supervisor-service-group")
    public void handleAppointmentScheduled(Map<String, Object> event) {
        try {
            log.info("Received appointment scheduled event: {}", event);
            
            Long appointmentId = Long.valueOf(event.get("appointmentId").toString());
            Long caseId = Long.valueOf(event.get("caseId").toString());
            String appointmentDateTime = event.get("appointmentDateTime").toString();
            
            log.info("Appointment {} scheduled for case {} at {}", appointmentId, caseId, appointmentDateTime);
            
            // TODO: Send notification to supervisor
            
        } catch (Exception e) {
            log.error("Error handling appointment scheduled event", e);
        }
    }
    
    /**
     * Listen to appointment completed events from doctor-service
     */
    @KafkaListener(topics = "appointment.completed", groupId = "supervisor-service-group")
    public void handleAppointmentCompleted(Map<String, Object> event) {
        try {
            log.info("Received appointment completed event: {}", event);
            
            Long appointmentId = Long.valueOf(event.get("appointmentId").toString());
            Long caseId = Long.valueOf(event.get("caseId").toString());
            
            log.info("Appointment {} completed for case {}", appointmentId, caseId);
            
            // TODO: Send notification to supervisor
            
        } catch (Exception e) {
            log.error("Error handling appointment completed event", e);
        }
    }
    
    /**
     * Listen to payment confirmation events from payment-service
     */
    @KafkaListener(topics = "payment.confirmed", groupId = "supervisor-service-group")
    public void handlePaymentConfirmed(Map<String, Object> event) {
        try {
            log.info("Received payment confirmed event: {}", event);
            
            Long paymentId = Long.valueOf(event.get("paymentId").toString());
            Long caseId = Long.valueOf(event.get("caseId").toString());
            String amount = event.get("amount").toString();
            String paymentMethod = event.get("paymentMethod").toString();
            
            log.info("Payment {} confirmed for case {} - amount: {}, method: {}", 
                    paymentId, caseId, amount, paymentMethod);
            
            // TODO: Update payment statistics
            
        } catch (Exception e) {
            log.error("Error handling payment confirmed event", e);
        }
    }
    
    /**
     * Listen to medical report generated events from doctor-service
     */
    @KafkaListener(topics = "medical-report.generated", groupId = "supervisor-service-group")
    public void handleMedicalReportGenerated(Map<String, Object> event) {
        try {
            log.info("Received medical report generated event: {}", event);
            
            Long caseId = Long.valueOf(event.get("caseId").toString());
            String reportUrl = event.get("reportUrl").toString();
            
            log.info("Medical report generated for case {} - URL: {}", caseId, reportUrl);
            
            // TODO: Send notification to supervisor
            
        } catch (Exception e) {
            log.error("Error handling medical report generated event", e);
        }
    }
}