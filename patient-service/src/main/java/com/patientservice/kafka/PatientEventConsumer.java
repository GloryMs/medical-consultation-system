package com.patientservice.kafka;

import com.patientservice.service.PatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PatientEventConsumer {

    private final PatientService patientService;

    @KafkaListener(topics = "payment-completed-topic", groupId = "patient-group")
    public void handlePaymentCompleted(Map<String, Object> paymentEvent) {
        try {
            Long patientId = Long.valueOf(paymentEvent.get("patientId").toString());
            Long caseId = paymentEvent.get("caseId") != null ? 
                Long.valueOf(paymentEvent.get("caseId").toString()) : null;
            String paymentType = paymentEvent.get("paymentType").toString();
            Double amount = Double.valueOf(paymentEvent.get("amount").toString());
            
            log.info("Payment completed for patient: {}, case: {}, type: {}, amount: {}", 
                    patientId, caseId, paymentType, amount);

            // Handle different payment types
            if ("SUBSCRIPTION".equals(paymentType)) {
                patientService.activateSubscriptionAfterPayment(patientId);
            } else if ("CONSULTATION".equals(paymentType) && caseId != null) {
                //patientService.updateCaseAfterPayment(caseId, patientId);
            }
            
        } catch (Exception e) {
            log.error("Error processing payment completed event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "user-registration-topic", groupId = "patient-group")
    public void handleUserRegistration(Map<String, Object> registrationEvent) {
        try {
            Long userId = Long.valueOf(registrationEvent.get("userId").toString());
            String email = registrationEvent.get("email").toString();
            String role = registrationEvent.get("role").toString();
            String fullName = registrationEvent.get("fullName").toString();
            
            log.info("User registration event received: {}, role: {}", email, role);

            System.out.println("Start handling kafka event of new user registration: " + email);
            
            // Only handle patient registrations
            if ("PATIENT".equals(role)) {
                patientService.initializePatientProfile(userId, email, fullName);
            }
            
        } catch (Exception e) {
            log.error("Error processing user registration event: {}", e.getMessage(), e);
        }
    }
}