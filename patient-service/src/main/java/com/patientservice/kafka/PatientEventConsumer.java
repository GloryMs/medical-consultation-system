package com.patientservice.kafka;

import com.commonlibrary.dto.CaseFeeUpdateEvent;
import com.patientservice.service.PatientService;
import com.patientservice.service.SmartCaseAssignmentService;
import com.patientservice.util.CustomLocalDateTimeParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
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
            String phoneNumber = registrationEvent.get("phoneNumber").toString();
            String role = registrationEvent.get("role").toString();
            String fullName = registrationEvent.get("fullName").toString();
            
            log.info("User registration event received: {}, phoneNumber {},  role: {}", email, phoneNumber, role);

            System.out.println("Start handling kafka event of new user registration: " + email);
            
            // Only handle patient registrations
            if ("PATIENT".equals(role)) {
                patientService.initializePatientProfile(userId, email, fullName, phoneNumber);
            }
            
        } catch (Exception e) {
            log.error("Error processing user registration event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "case-fee-update-topic", groupId = "patient-group")
    public void handleCaseFeeUpdate(Map<String, Object> feeEvent) {
        try
        {

            Long caseId = feeEvent.get("caseId") != null ?
                    Long.valueOf(feeEvent.get("caseId").toString()) : null;
//            Long doctorId = Long.valueOf(feeEvent.get("doctorId").toString());
//            Long doctor = Long.valueOf( feeEvent.get("doctorUserId").toString());
//            Long patientId = Long.valueOf(feeEvent.get("patientId").toString());
//            Long patientUserId = Long.valueOf(  feeEvent.get("patientUserId").toString());
            BigDecimal consultationFee = feeEvent.get("consultationFee") != null?
                    BigDecimal.valueOf(Long.parseLong(feeEvent.get("consultationFee").toString())) : null;

            LocalDateTime feeSetAt;
            String feeSetAtString = feeEvent.get("feeSetAt").toString();
            try {
                feeSetAt = CustomLocalDateTimeParser.parseCustomFormat(feeSetAtString);
                System.out.println("Successfully parsed feeSetAt: " + feeSetAt);
            } catch (IllegalArgumentException e) {
                feeSetAt = LocalDateTime.now();
                System.err.println("Failed to parse feeSetAt: " + e.getMessage());
                // Handle the error appropriately, e.g., log it, return a default value, etc.
            }
            //feeEvent.get("eventType");


            log.info("Received case fee update event for case: {} with fee: ${}", caseId, consultationFee);

            patientService.updateCaseConsultationFee(caseId,consultationFee, feeSetAt);

            log.info("Successfully updated consultation fee for case: {}", caseId);

        } catch (Exception e) {
            log.error("Error processing case fee update event for case : {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "case-status-doctor-updated-topic", groupId = "patient-group")
    public void handleCaseStatusUpdateFromDoctor(Map<String, Object> caseEvent) {
        try {
            Long caseId = Long.valueOf(caseEvent.get("caseId").toString());
            String newStatus = caseEvent.get("newStatus").toString();
            Long doctorId = caseEvent.get("doctorId") != null ?
                    Long.valueOf(caseEvent.get("doctorId").toString()) : null;

            log.info("Kafka Patient Listener: Case status updated: case={}, status={}, doctor={}",
                    caseId, newStatus, doctorId);

            patientService.updateCaseStatus(caseId, newStatus, doctorId);

        } catch (Exception e) {
            log.error("Error processing case status update: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "case-report-update-topic", groupId = "patient-group")
    public void handleMedicalReportReadiness(Map<String, Object> caseEvent) {
        try {
            Long caseId = Long.valueOf(caseEvent.get("caseId").toString());
            String pdfUrl = caseEvent.get("pdfUrl").toString();
            Long doctorId = caseEvent.get("doctorId") != null ?
                    Long.valueOf(caseEvent.get("doctorId").toString()) : null;
            Long patientId = caseEvent.get("patientId") != null ?
                    Long.valueOf(caseEvent.get("patientId").toString()) : null;
            Long reportId = caseEvent.get("reportId") != null ?
                    Long.valueOf(caseEvent.get("reportId").toString()) : null;

            log.info("Kafka Patient Listener: Case medical report readiness: case={}, PDF URL={}, doctor={}, patient={}",
                    caseId, pdfUrl, doctorId, patientId);

            patientService.updateCaseForMedicalReport(caseId, pdfUrl, patientId, reportId);

        } catch (Exception e) {
            log.error("Error processing medical report update: {}", e.getMessage(), e);
        }
    }
}