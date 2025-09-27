package com.patientservice.kafka;

import com.commonlibrary.dto.CaseFeeUpdateEvent;
import com.patientservice.service.PatientService;
import com.patientservice.service.SmartCaseAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PatientEventConsumer {

    private final PatientService patientService;
    private final SmartCaseAssignmentService assignmentService;

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

    @KafkaListener(topics = "case-create-assign-topic", groupId = "patient-group")
    public void handleCaseAssignmentTrigger(Map<String, Object> caseEvetn){
        try {
            Long caseId = caseEvetn.get("caseId") != null ?
                    Long.valueOf(caseEvetn.get("caseId").toString()) : null;
            //TODO  this sleep must be replaced with validation to insure that case was inserted ok.
            Thread.sleep(1000); // Small delay to ensure transaction is committed
            System.out.println("Kafka - A new Case has been added, Case#: " + caseId + "\n");
            System.out.println("Kafka - Smart Case Assignment started asynchronously @: " +
                    LocalDateTime.now() + "\n");
            assignmentService.assignCaseToMultipleDoctors(caseId);
        } catch (Exception e) {
            System.out.println("Kafka - Failed to handle assign new case automatically");
            System.out.println(e.getMessage());
            e.printStackTrace();
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

    @KafkaListener(topics = "case-fee-update-topic", groupId = "patient-service-group")
    public void handleCaseFeeUpdate(CaseFeeUpdateEvent event) {
        try {
            log.info("Received case fee update event for case: {} with fee: ${}",
                    event.getCaseId(), event.getConsultationFee());

            patientService.updateCaseConsultationFee(event.getCaseId(),
                    event.getConsultationFee(), event.getFeeSetAt());

            log.info("Successfully updated consultation fee for case: {}", event.getCaseId());

        } catch (Exception e) {
            log.error("Error processing case fee update event for case {}: {}",
                    event.getCaseId(), e.getMessage(), e);
        }
    }
}