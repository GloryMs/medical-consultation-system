package com.adminservice.kafka;

import com.adminservice.service.AdminService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

import static com.adminservice.util.CustomLocalDateTimeParser.parseCustomFormat;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminEventConsumer {

    private final AdminService adminService;

    @KafkaListener(topics = "user-registration-topic",groupId = "admin-group")
    public void handleUserRegistration(Map<String, Object> registrationEvent) {
        try {
            Long userId = Long.valueOf(registrationEvent.get("userId").toString());
            String email = registrationEvent.get("email").toString();
            String role = registrationEvent.get("role").toString();
            
            log.info("New user registered: {}, role: {}", email, role);
            
            // Send admin notification for new doctor registrations
            if ("DOCTOR".equals(role)) {
                adminService.notifyAdminOfNewDoctor(userId, email);
            }
            
        } catch (Exception e) {
            log.error("Error processing user registration event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "payment-completed-topic", groupId = "admin-group")
    public void handlePaymentCompleted(Map<String, Object> paymentEvent) {
        try {
            String paymentType = paymentEvent.get("paymentType").toString();
            Double amount = Double.valueOf(paymentEvent.get("amount").toString());
            
            log.info("Payment completed: type={}, amount={}", paymentType, amount);
            
            // Update financial statistics
            adminService.updatePaymentStats(paymentType, amount);
            
        } catch (Exception e) {
            log.error("Error processing payment event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "case-appointment-cancellation-topic", groupId = "admin-group")
    public void handleCaseCancellation(Map<String, Object> appointmentCancellationEvent) {
        try {
            Long appointmentId = Long.valueOf(appointmentCancellationEvent.get("appointmentId").toString());
            Long caseId = Long.valueOf(appointmentCancellationEvent.get("caseId").toString());
            Long doctorId = Long.valueOf(appointmentCancellationEvent.get("doctorId").toString());
            Long patientId = Long.valueOf(appointmentCancellationEvent.get("patientId").toString());
            String reason = appointmentCancellationEvent.get("reason").toString();
            LocalDateTime appointmentTime = parseCustomFormat(appointmentCancellationEvent.
                    get("appointmentTime").toString());
            //parseCustomFormat

            log.info("Appointment {} that was scheduled on {}, had been cancelled by doctor {},"+
                            " for case {} and patient {}",
                    appointmentId, appointmentTime, doctorId, caseId, patientId);

            // Todo you must process refund:

        } catch (Exception e) {
            log.error("Error processing refund regarding cancelled appointment: {}", e.getMessage(), e);
        }
    }
}