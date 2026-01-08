package com.supervisorservice.kafka;

import com.supervisorservice.entity.SupervisorPatientAssignment;
import com.supervisorservice.repository.SupervisorPatientAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Kafka consumer for supervisor service events from other services
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SupervisorKafkaConsumer {

    private final SupervisorPatientAssignmentRepository assignmentRepository;

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
    /// /////////////////////////////

    /**
     * Handle appointment confirmed event
     */
    @KafkaListener(topics = "appointment.confirmed", groupId = "supervisor-service-group")
    public void handleAppointmentConfirmed(Map<String, Object> event) {
        try {
            Long patientId = Long.valueOf(event.get("patientId").toString());
            Long caseId = Long.valueOf(event.get("caseId").toString());
            Long appointmentId = Long.valueOf(event.get("appointmentId").toString());

            log.info("Received appointment.confirmed event - Appointment: {}, Patient: {}, Case: {}",
                    appointmentId, patientId, caseId);

            // Check if this patient is managed by a supervisor
            Optional<SupervisorPatientAssignment> assignment = findActiveAssignmentByPatientId(patientId);

            if (assignment.isPresent()) {
                Long supervisorId = assignment.get().getSupervisor().getId();
                log.info("Appointment {} confirmed for supervisor-managed patient {}",
                        appointmentId, patientId);

                // TODO: Update any supervisor-side tracking or send notification
            }

        } catch (Exception e) {
            log.error("Error handling appointment.confirmed event", e);
        }
    }

    /**
     * Handle appointment cancelled event from doctor-service
     */
    @KafkaListener(topics = "appointment.cancelled", groupId = "supervisor-service-group")
    public void handleAppointmentCancelled(Map<String, Object> event) {
        try {
            Long appointmentId = Long.valueOf(event.get("appointmentId").toString());
            Long patientId = Long.valueOf(event.get("patientId").toString());
            Long caseId = Long.valueOf(event.get("caseId").toString());
            String reason = event.containsKey("reason") ? event.get("reason").toString() : "No reason provided";

            log.info("Received appointment.cancelled event - Appointment: {}, Patient: {}, Reason: {}",
                    appointmentId, patientId, reason);

            // Check if this patient is managed by a supervisor
            Optional<SupervisorPatientAssignment> assignment = findActiveAssignmentByPatientId(patientId);

            if (assignment.isPresent()) {
                Long supervisorId = assignment.get().getSupervisor().getId();
                log.info("Appointment {} cancelled for supervisor {} managed patient {}. Reason: {}",
                        appointmentId, supervisorId, patientId, reason);

                // TODO: Send urgent notification to supervisor about cancellation
            }

        } catch (Exception e) {
            log.error("Error handling appointment.cancelled event", e);
        }
    }

    /**
     * Handle appointment completed event
     */
    @KafkaListener(topics = "appointment.completed", groupId = "supervisor-service-group")
    public void handleAppointmentCompleted(Map<String, Object> event) {
        try {
            Long appointmentId = Long.valueOf(event.get("appointmentId").toString());
            Long patientId = Long.valueOf(event.get("patientId").toString());
            Long caseId = Long.valueOf(event.get("caseId").toString());

            log.info("Received appointment.completed event - Appointment: {}, Patient: {}, Case: {}",
                    appointmentId, patientId, caseId);

            // Check if this patient is managed by a supervisor
            Optional<SupervisorPatientAssignment> assignment = findActiveAssignmentByPatientId(patientId);

            if (assignment.isPresent()) {
                Long supervisorId = assignment.get().getSupervisor().getId();
                log.info("Appointment {} completed for supervisor-managed patient {}",
                        appointmentId, patientId);

                // TODO: Update supervisor dashboard metrics
                // TODO: Send notification about completed appointment
            }

        } catch (Exception e) {
            log.error("Error handling appointment.completed event", e);
        }
    }

    /**
     * Handle appointment rescheduled event
     */
    @KafkaListener(topics = "appointment.rescheduled", groupId = "supervisor-service-group")
    public void handleAppointmentRescheduled(Map<String, Object> event) {
        try {
            Long appointmentId = Long.valueOf(event.get("appointmentId").toString());
            Long patientId = Long.valueOf(event.get("patientId").toString());
            String newTime = event.get("newScheduledTime").toString();
            String previousTime = event.containsKey("previousTime") ? event.get("previousTime").toString() : null;

            log.info("Received appointment.rescheduled event - Appointment: {}, Patient: {}, New Time: {}",
                    appointmentId, patientId, newTime);

            // Check if this patient is managed by a supervisor
            Optional<SupervisorPatientAssignment> assignment = findActiveAssignmentByPatientId(patientId);

            if (assignment.isPresent()) {
                Long supervisorId = assignment.get().getSupervisor().getId();
                log.info("Appointment {} rescheduled for supervisor {} managed patient {} to {}",
                        appointmentId, supervisorId, patientId, newTime);

                // TODO: Send notification to supervisor about rescheduled appointment
            }

        } catch (Exception e) {
            log.error("Error handling appointment.rescheduled event", e);
        }
    }

    /**
     * Handle reschedule request status update
     */
    @KafkaListener(topics = "reschedule.request.updated", groupId = "supervisor-service-group")
    public void handleRescheduleRequestUpdated(Map<String, Object> event) {
        try {
            Long requestId = Long.valueOf(event.get("requestId").toString());
            Long patientId = Long.valueOf(event.get("patientId").toString());
            String status = event.get("status").toString();

            log.info("Received reschedule.request.updated event - Request: {}, Patient: {}, Status: {}",
                    requestId, patientId, status);

            // Check if this patient is managed by a supervisor
            Optional<SupervisorPatientAssignment> assignment = findActiveAssignmentByPatientId(patientId);

            if (assignment.isPresent()) {
                Long supervisorId = assignment.get().getSupervisor().getId();
                log.info("Reschedule request {} for supervisor-managed patient {} updated to status: {}",
                        requestId, patientId, status);

                // TODO: Send notification about reschedule request status change
            }

        } catch (Exception e) {
            log.error("Error handling reschedule.request.updated event", e);
        }
    }

    /**
     * Handle case status update event
     */
    @KafkaListener(topics = "case.status.updated", groupId = "supervisor-service-group")
    public void handleCaseStatusUpdated(Map<String, Object> event) {
        try {
            Long caseId = Long.valueOf(event.get("caseId").toString());
            Long patientId = Long.valueOf(event.get("patientId").toString());
            String oldStatus = event.containsKey("oldStatus") ? event.get("oldStatus").toString() : null;
            String newStatus = event.get("newStatus").toString();

            log.info("Received case.status.updated event - Case: {}, Patient: {}, Status: {} -> {}",
                    caseId, patientId, oldStatus, newStatus);

            // Check if this patient is managed by a supervisor
            Optional<SupervisorPatientAssignment> assignment = findActiveAssignmentByPatientId(patientId);

            if (assignment.isPresent()) {
                Long supervisorId = assignment.get().getSupervisor().getId();
                log.info("Case {} status changed for supervisor {} managed patient {}: {} -> {}",
                        caseId, supervisorId, patientId, oldStatus, newStatus);

                // TODO: Update supervisor dashboard
                // TODO: Send notification based on status change (e.g., case assigned, scheduled, completed)
            }

        } catch (Exception e) {
            log.error("Error handling case.status.updated event", e);
        }
    }

    /**
     * Handle appointment reminder event
     */
    @KafkaListener(topics = "appointment.reminder", groupId = "supervisor-service-group")
    public void handleAppointmentReminder(Map<String, Object> event) {
        try {
            Long appointmentId = Long.valueOf(event.get("appointmentId").toString());
            Long patientId = Long.valueOf(event.get("patientId").toString());
            String scheduledTime = event.get("scheduledTime").toString();
            Integer minutesUntil = Integer.valueOf(event.get("minutesUntil").toString());

            log.info("Received appointment.reminder event - Appointment: {} in {} minutes",
                    appointmentId, minutesUntil);

            // Check if this patient is managed by a supervisor
            Optional<SupervisorPatientAssignment> assignment = findActiveAssignmentByPatientId(patientId);

            if (assignment.isPresent()) {
                Long supervisorId = assignment.get().getSupervisor().getId();
                log.info("Appointment reminder for supervisor {} - Appointment {} in {} minutes",
                        supervisorId, appointmentId, minutesUntil);

                // TODO: Send reminder notification to supervisor
            }

        } catch (Exception e) {
            log.error("Error handling appointment.reminder event", e);
        }
    }

    // Helper method to find active assignment by patient ID
    private Optional<SupervisorPatientAssignment> findActiveAssignmentByPatientId(Long patientId) {
        return assignmentRepository.findByPatientIdAndAssignmentStatus(
                patientId,
                com.commonlibrary.entity.SupervisorAssignmentStatus.ACTIVE);
    }
}