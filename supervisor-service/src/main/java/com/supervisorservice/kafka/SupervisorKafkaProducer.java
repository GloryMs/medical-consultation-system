package com.supervisorservice.kafka;

import com.commonlibrary.dto.NotificationDto;
import com.commonlibrary.entity.NotificationType;
import com.supervisorservice.entity.MedicalSupervisor;
import com.supervisorservice.entity.SupervisorCoupon;
import com.supervisorservice.entity.SupervisorPatientAssignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka producer for supervisor service events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SupervisorKafkaProducer {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    // Topic names
    private static final String SUPERVISOR_REGISTERED = "supervisor.registered";
    private static final String SUPERVISOR_VERIFIED = "supervisor.verified";
    private static final String SUPERVISOR_SUSPENDED = "supervisor.suspended";
    private static final String PATIENT_ASSIGNED = "supervisor.patient.assigned";
    private static final String PATIENT_REMOVED = "supervisor.patient.removed";
    private static final String CASE_SUBMITTED = "supervisor.case.submitted";
    private static final String COUPON_ISSUED = "coupon.issued";
    private static final String COUPON_REDEEMED = "coupon.redeemed";
    private static final String COUPON_EXPIRED = "coupon.expired";
    private static final String COUPON_CANCELLED = "coupon.cancelled";
    private static final String COUPON_EXPIRING_SOON = "coupon.expiring-soon";
    
    /**
     * Send supervisor registered event
     */
    public void sendSupervisorRegisteredEvent(MedicalSupervisor supervisor) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "SUPERVISOR_REGISTERED");
            event.put("supervisorId", supervisor.getId());
            event.put("userId", supervisor.getUserId());
            event.put("email", supervisor.getEmail());
            event.put("fullName", supervisor.getFullName());
            event.put("timestamp", LocalDateTime.now().toString());
            
            kafkaTemplate.send(SUPERVISOR_REGISTERED, supervisor.getId().toString(), event);
            log.info("Sent supervisor registered event for supervisorId: {}", supervisor.getId());
        } catch (Exception e) {
            log.error("Error sending supervisor registered event", e);
        }
    }
    
    /**
     * Send supervisor verified event
     */
    public void sendSupervisorVerifiedEvent(MedicalSupervisor supervisor, Long verifiedBy) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "SUPERVISOR_VERIFIED");
            event.put("supervisorId", supervisor.getId());
            event.put("userId", supervisor.getUserId());
            event.put("email", supervisor.getEmail());
            event.put("verifiedBy", verifiedBy);
            event.put("verifiedAt", LocalDateTime.now().toString());
            event.put("timestamp", LocalDateTime.now().toString());
            
            kafkaTemplate.send(SUPERVISOR_VERIFIED, supervisor.getId().toString(), event);
            log.info("Sent supervisor verified event for supervisorId: {}", supervisor.getId());
        } catch (Exception e) {
            log.error("Error sending supervisor verified event", e);
        }
    }
    
    /**
     * Send supervisor suspended event
     */
    public void sendSupervisorSuspendedEvent(MedicalSupervisor supervisor, String reason) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "SUPERVISOR_SUSPENDED");
            event.put("supervisorId", supervisor.getId());
            event.put("userId", supervisor.getUserId());
            event.put("reason", reason);
            event.put("timestamp", LocalDateTime.now().toString());
            
            kafkaTemplate.send(SUPERVISOR_SUSPENDED, supervisor.getId().toString(), event);
            log.info("Sent supervisor suspended event for supervisorId: {}", supervisor.getId());
        } catch (Exception e) {
            log.error("Error sending supervisor suspended event", e);
        }
    }
    
    /**
     * Send patient assigned event
     */
    public void sendPatientAssignedEvent(SupervisorPatientAssignment assignment) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "PATIENT_ASSIGNED");
            event.put("assignmentId", assignment.getId());
            event.put("supervisorId", assignment.getSupervisor().getId());
            event.put("patientId", assignment.getPatientId());
            event.put("assignedAt", assignment.getAssignedAt().toString());
            event.put("timestamp", LocalDateTime.now().toString());
            
            kafkaTemplate.send(PATIENT_ASSIGNED, assignment.getPatientId().toString(), event);
            log.info("Sent patient assigned event - supervisorId: {}, patientId: {}", 
                    assignment.getSupervisor().getId(), assignment.getPatientId());
        } catch (Exception e) {
            log.error("Error sending patient assigned event", e);
        }
    }
    
    /**
     * Send patient removed event
     */
    public void sendPatientRemovedEvent(SupervisorPatientAssignment assignment, String reason) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "PATIENT_REMOVED");
            event.put("assignmentId", assignment.getId());
            event.put("supervisorId", assignment.getSupervisor().getId());
            event.put("patientId", assignment.getPatientId());
            event.put("reason", reason);
            event.put("timestamp", LocalDateTime.now().toString());
            
            kafkaTemplate.send(PATIENT_REMOVED, assignment.getPatientId().toString(), event);
            log.info("Sent patient removed event - supervisorId: {}, patientId: {}", 
                    assignment.getSupervisor().getId(), assignment.getPatientId());
        } catch (Exception e) {
            log.error("Error sending patient removed event", e);
        }
    }


    
    /**
     * Send case submitted event
     */
    public void sendCaseSubmittedEvent(Long supervisorId, Long patientId, Long caseId) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "CASE_SUBMITTED");
            event.put("supervisorId", supervisorId);
            event.put("patientId", patientId);
            event.put("caseId", caseId);
            event.put("timestamp", LocalDateTime.now().toString());
            
            kafkaTemplate.send(CASE_SUBMITTED, caseId.toString(), event);
            log.info("Sent case submitted event - caseId: {}", caseId);
        } catch (Exception e) {
            log.error("Error sending case submitted event", e);
        }
    }
    
    /**
     * Send coupon issued event
     */
    public void sendCouponIssuedEvent(SupervisorCoupon coupon) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "COUPON_ISSUED");
            event.put("couponId", coupon.getId());
            event.put("couponCode", coupon.getCouponCode());
            event.put("supervisorId", coupon.getSupervisor().getId());
            event.put("patientId", coupon.getPatientId());
            event.put("amount", coupon.getAmount().toString());
            event.put("expiresAt", coupon.getExpiresAt().toString());
            event.put("timestamp", LocalDateTime.now().toString());
            
            kafkaTemplate.send(COUPON_ISSUED, coupon.getId().toString(), event);
            log.info("Sent coupon issued event - couponCode: {}", coupon.getCouponCode());
        } catch (Exception e) {
            log.error("Error sending coupon issued event", e);
        }
    }
    
    /**
     * Send coupon redeemed event
     */
    public void sendCouponRedeemedEvent(SupervisorCoupon coupon, Long caseId, Long paymentId) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "COUPON_REDEEMED");
            event.put("couponId", coupon.getId());
            event.put("couponCode", coupon.getCouponCode());
            event.put("supervisorId", coupon.getSupervisor().getId());
            event.put("patientId", coupon.getPatientId());
            event.put("caseId", caseId);
            event.put("paymentId", paymentId);
            event.put("amount", coupon.getAmount().toString());
            event.put("timestamp", LocalDateTime.now().toString());
            
            kafkaTemplate.send(COUPON_REDEEMED, coupon.getId().toString(), event);
            log.info("Sent coupon redeemed event - couponCode: {}", coupon.getCouponCode());
        } catch (Exception e) {
            log.error("Error sending coupon redeemed event", e);
        }
    }
    
    /**
     * Send coupon expired event
     */
    public void sendCouponExpiredEvent(SupervisorCoupon coupon) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "COUPON_EXPIRED");
            event.put("couponId", coupon.getId());
            event.put("couponCode", coupon.getCouponCode());
            event.put("supervisorId", coupon.getSupervisor().getId());
            event.put("patientId", coupon.getPatientId());
            event.put("timestamp", LocalDateTime.now().toString());
            
            kafkaTemplate.send(COUPON_EXPIRED, coupon.getId().toString(), event);
            log.info("Sent coupon expired event - couponCode: {}", coupon.getCouponCode());
        } catch (Exception e) {
            log.error("Error sending coupon expired event", e);
        }
    }
    
    /**
     * Send coupon cancelled event
     */
    public void sendCouponCancelledEvent(SupervisorCoupon coupon, String reason) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "COUPON_CANCELLED");
            event.put("couponId", coupon.getId());
            event.put("couponCode", coupon.getCouponCode());
            event.put("supervisorId", coupon.getSupervisor().getId());
            event.put("patientId", coupon.getPatientId());
            event.put("reason", reason);
            event.put("timestamp", LocalDateTime.now().toString());
            
            kafkaTemplate.send(COUPON_CANCELLED, coupon.getId().toString(), event);
            log.info("Sent coupon cancelled event - couponCode: {}", coupon.getCouponCode());
        } catch (Exception e) {
            log.error("Error sending coupon cancelled event", e);
        }
    }
    
    /**
     * Send coupon expiring soon event
     */
    public void sendCouponExpiringSoonEvent(SupervisorCoupon coupon, int daysUntilExpiry) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "COUPON_EXPIRING_SOON");
            event.put("couponId", coupon.getId());
            event.put("couponCode", coupon.getCouponCode());
            event.put("supervisorId", coupon.getSupervisor().getId());
            event.put("patientId", coupon.getPatientId());
            event.put("daysUntilExpiry", daysUntilExpiry);
            event.put("expiresAt", coupon.getExpiresAt().toString());
            event.put("timestamp", LocalDateTime.now().toString());
            
            kafkaTemplate.send(COUPON_EXPIRING_SOON, coupon.getId().toString(), event);
            log.info("Sent coupon expiring soon event - couponCode: {}", coupon.getCouponCode());
        } catch (Exception e) {
            log.error("Error sending coupon expiring soon event", e);
        }
    }

    // ==================== Appointment Events ====================

    /**
     * Send appointment accepted event
     */
    public void sendAppointmentAcceptedEvent(Long supervisorId, Long patientId, Long caseId, String notes) {
        Map<String, Object> event = new HashMap<>();
        event.put("supervisorId", supervisorId);
        event.put("patientId", patientId);
        event.put("caseId", caseId);
        event.put("notes", notes);
        event.put("timestamp", LocalDateTime.now().toString());

        kafkaTemplate.send("supervisor.appointment.accepted", event);
        log.info("Sent supervisor.appointment.accepted event for case {} by supervisor {}", caseId, supervisorId);
    }


    public void sendScheduleConfirmationEvent(Long caseId, Long patientId, Long doctorId) {
        // Send notification to Doctor about appointment confirmation
        NotificationDto patientNotification = NotificationDto.builder()
                .senderId(patientId != null ? patientId : 0L)
                .receiverId(doctorId)
                .title("Case appointment Confirmation")
                .message("Patient has confirmed the appointment for case #" + caseId )
                .type(NotificationType.APPOINTMENT)
                .sendEmail(true)
                .build();

        kafkaTemplate.send("notification-topic", patientNotification);

        // Send case status event
        Map<String, Object> appointmentConfirmationEvent = new HashMap<>();
        appointmentConfirmationEvent.put("caseId", caseId);
        appointmentConfirmationEvent.put("patientId", patientId);
        appointmentConfirmationEvent.put("doctorId", doctorId);
        appointmentConfirmationEvent.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send("case-appointment-confirmation-topic", appointmentConfirmationEvent);
        log.info("Kafka - Appointment confirmation for case: {} , by patient {}, and doctor", caseId, patientId);
    }

    /**
     * Send reschedule request created event
     */
    public void sendRescheduleRequestCreatedEvent(
            Long supervisorId,
            Long patientId,
            Long caseId,
            Long appointmentId,
            LocalDateTime requestedTime,
            String reason) {

        Map<String, Object> event = new HashMap<>();
        event.put("supervisorId", supervisorId);
        event.put("patientId", patientId);
        event.put("caseId", caseId);
        event.put("appointmentId", appointmentId);
        event.put("requestedTime", requestedTime.toString());
        event.put("reason", reason);
        event.put("timestamp", LocalDateTime.now().toString());

        kafkaTemplate.send("supervisor.reschedule.requested", event);
        log.info("Sent supervisor.reschedule.requested event for appointment {} by supervisor {}",
                appointmentId, supervisorId);
    }

    // ==================== Payment Events ====================

    /**
     * Send payment completed event
     */
    public void sendPaymentCompletedEvent(
            Long supervisorId,
            Long patientId,
            Long caseId,
            BigDecimal amount,
            String paymentMethod,
            String transactionId) {

        Map<String, Object> event = new HashMap<>();
        event.put("supervisorId", supervisorId);
        event.put("patientId", patientId);
        event.put("caseId", caseId);
        event.put("amount", amount.toString());
        event.put("paymentMethod", paymentMethod);
        event.put("transactionId", transactionId);
        event.put("timestamp", LocalDateTime.now().toString());

        kafkaTemplate.send("supervisor.payment.completed", event);
        log.info("Sent supervisor.payment.completed event for case {} amount {}", caseId, amount);
    }
}
