package com.doctorservice.kafka;

import com.commonlibrary.entity.CaseStatus;
import com.doctorservice.service.DoctorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DoctorEventConsumer {

    private final DoctorService doctorService;

    @KafkaListener(topics = "case-status-updated-topic", groupId = "doctor-group")
    public void handleCaseStatusUpdate(Map<String, Object> caseEvent) {
        try {
            Long caseId = Long.valueOf(caseEvent.get("caseId").toString());
            String newStatus = caseEvent.get("newStatus").toString();
            Long doctorId = caseEvent.get("doctorId") != null ? 
                Long.valueOf(caseEvent.get("doctorId").toString()) : null;
            
            log.info("Case status updated: case={}, status={}, doctor={}", caseId, newStatus, doctorId);
            
            // Update doctor's workload or case assignments
            if (doctorId != null && "ACCEPTED".equals(newStatus)) {
                doctorService.updateDoctorWorkLoad(doctorId, CaseStatus.ACCEPTED, 1);
            } else if (doctorId != null && "ASSIGNED".equals(newStatus)) {
                doctorService.updateDoctorWorkLoad(doctorId, CaseStatus.ASSIGNED, 1);
            }
            
        } catch (Exception e) {
            log.error("Error processing case status update: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "user-registration-topic", groupId = "doctor-group")
    public void handleUserRegistration(Map<String, Object> registrationEvent) {
        try {
            Long userId = Long.valueOf(registrationEvent.get("userId").toString());
            String email = registrationEvent.get("email").toString();
            String role = registrationEvent.get("role").toString();
            
            log.info("User registration event received: {}, role: {}", email, role);
            
            // Only handle doctor registrations
            if ("DOCTOR".equals(role)) {
                doctorService.initializeDoctorProfile(userId, email);
            }
            
        } catch (Exception e) {
            log.error("Error processing user registration event: {}", e.getMessage(), e);
        }
    }
}