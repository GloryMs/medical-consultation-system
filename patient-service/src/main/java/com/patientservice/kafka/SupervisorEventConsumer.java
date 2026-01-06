package com.patientservice.kafka;

import com.patientservice.entity.Patient;
import com.patientservice.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes events from supervisor-service
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SupervisorEventConsumer {
    
    private final PatientRepository patientRepository;
    
    /**
     * Handle patient assigned to supervisor
     */
    @KafkaListener(topics = "supervisor.patient.assigned", groupId = "patient-service-group")
    public void handlePatientAssigned(Map<String, Object> event) {
        try {
            Long supervisorId = Long.valueOf(event.get("supervisorId").toString());
            Long patientId = Long.valueOf(event.get("patientId").toString());
            
            log.info("Patient {} assigned to supervisor {}", patientId, supervisorId);
            
            // Update patient record
            Patient patient = patientRepository.findById(patientId).orElse(null);
            if (patient != null) {
                patient.setIsSupervisorManaged(true);
                patientRepository.save(patient);
                log.info("Patient record updated with supervisor management flag");
            }
            
        } catch (Exception e) {
            log.error("Error handling patient assigned event", e);
        }
    }
    
    /**
     * Handle patient removed from supervisor
     */
    @KafkaListener(topics = "supervisor.patient.removed", groupId = "patient-service-group")
    public void handlePatientRemoved(Map<String, Object> event) {
        try {
            Long supervisorId = Long.valueOf(event.get("supervisorId").toString());
            Long patientId = Long.valueOf(event.get("patientId").toString());
            String reason = event.get("reason").toString();
            
            log.info("Patient {} removed from supervisor {} - reason: {}", 
                    patientId, supervisorId, reason);
            
            // Update patient record
            Patient patient = patientRepository.findById(patientId).orElse(null);
            if (patient != null) {
                patient.setIsSupervisorManaged(false);
                patientRepository.save(patient);
                log.info("Patient record updated - supervisor management removed");
            }
            
        } catch (Exception e) {
            log.error("Error handling patient removed event", e);
        }
    }
    
    /**
     * Handle case submitted by supervisor
     */
    @KafkaListener(topics = "supervisor.case.submitted", groupId = "patient-service-group")
    public void handleCaseSubmitted(Map<String, Object> event) {
        try {
            Long supervisorId = Long.valueOf(event.get("supervisorId").toString());
            Long patientId = Long.valueOf(event.get("patientId").toString());
            Long caseId = Long.valueOf(event.get("caseId").toString());
            
            log.info("Case {} submitted for patient {} by supervisor {}", 
                    caseId, patientId, supervisorId);
            
            // Case is already created - this is just for audit/notification
            // Could update case with supervisor info here if needed
            
        } catch (Exception e) {
            log.error("Error handling case submitted event", e);
        }
    }
}