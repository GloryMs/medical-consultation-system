package com.doctorservice.kafka;

import com.commonlibrary.dto.NotificationDto;
import com.commonlibrary.entity.NotificationPriority;
import com.commonlibrary.entity.NotificationType;
import com.doctorservice.entity.Doctor;
import com.doctorservice.feign.PatientServiceClient;
import com.doctorservice.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import com.commonlibrary.entity.UserType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DoctorEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final DoctorRepository doctorRepository;
    private final PatientServiceClient patientServiceClient;

    public void SendCaseScheduleUpdate(Long doctorId, Long patientId, Long caseId,
                                       LocalDateTime appointmentTime, String doctorName) {

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        // Send notification to notification service
        NotificationDto notification = NotificationDto.builder()
                .senderUserId(0L)  // System/Admin
                .receiverUserId(getPatientUserId(caseId, doctor.getId() ))
                .senderType(UserType.SYSTEM)
                .receiverType(UserType.PATIENT)
                .title("Appointment Scheduling")
                .message("An appointment has been scheduled for your case# " + caseId +
                        " at: " + appointmentTime +" ,with Doctor  " + doctorName )
                .type(NotificationType.APPOINTMENT)
                .sendEmail(true)
                .build();

        kafkaTemplate.send("notification-topic", notification);
        log.info("An appointment has been scheduled for your case# {}", caseId);
    }

    public void SendRejectRescheduleRequest(Long doctorId, Long patientId,
                                            Long caseId, String doctorName, String reason) {
        // Send notification to notification service

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        NotificationDto notification = NotificationDto.builder()
                .senderUserId(0L)  // System/Admin
                .receiverUserId(getPatientUserId(caseId, doctor.getId() ))
                .senderType(UserType.SYSTEM)
                .receiverType(UserType.PATIENT)
                .title("Reschedule Request Rejection")
                .message("Unfortunately Doctor: " + doctorName +
                        ", has rejected your re-schedule request for the case#  " + caseId
                        +", for the following reason: " + reason )
                .type(NotificationType.APPOINTMENT)
                .priority(NotificationPriority.HIGH)
                .sendEmail(true)
                .build();

        kafkaTemplate.send("notification-topic", notification);
        log.info("Doctor: {} has rejected your re-schedule request for the case#  {}, for the following reason: {} "
                ,doctorName, caseId, reason);
    }

    /**
     * Send case fee update event via Kafka
     */
    public void sendCaseFeeUpdateEvent(Long caseId, Long doctorId, Long doctorUserId, Long patientId,
                                       Long patientUserId, BigDecimal consultationFee) {
        try {
            // Create case fee update event
            Map<String, Object> feeEvent = new HashMap<>();
            feeEvent.put("caseId", caseId);
            feeEvent.put("doctorId", doctorId);
            feeEvent.put("doctorUserId", doctorUserId);
            feeEvent.put("patientId", patientId);
            feeEvent.put("patientUserId", patientUserId);
            feeEvent.put("consultationFee", consultationFee);
            feeEvent.put("feeSetAt", LocalDateTime.now());
            feeEvent.put("eventType", "CASE_FEE_SET");
            feeEvent.put("timestamp", System.currentTimeMillis());

            // Send event to update case in patient service
            kafkaTemplate.send("case-fee-update-topic", feeEvent);
            log.info("Case fee update event sent for case: {} with fee: ${}", caseId, consultationFee);

            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));

            // Send notification to patient about fee being set
            NotificationDto patientNotification = NotificationDto.builder()
                    .senderUserId(doctor.getUserId())
                    .receiverUserId(getPatientUserId(caseId, doctor.getId() ))
                    .senderType(UserType.DOCTOR)
                    .receiverType(UserType.PATIENT)
                    .title("Consultation Fee Set")
                    .message(String.format("Your doctor has set the consultation fee"+
                            " of $%.2f for your case: "+ caseId +
                            ". You have to wait for the doctor to schedule your appointment.", consultationFee))
                    .type(NotificationType.CASE)
                    .sendEmail(true)
                    .priority(NotificationPriority.MEDIUM)
                    .build();

            kafkaTemplate.send("notification-topic", patientNotification);
            log.info("Case fee notification sent to patient: {}", patientId);

        } catch (Exception e) {
            log.error("Error sending case fee update event for case {}: {}", caseId, e.getMessage(), e);
        }
    }

    public void sendAppointmentCancellationEvent (Long appointmentId, Long caseId,Long doctorId,
                                                  Long patientId, LocalDateTime appointmentTime, String reason) {
        try {
            // Create case fee update event
            Map<String, Object> appointmentCancellationEvent = new HashMap<>();
            appointmentCancellationEvent.put("appointmentId", appointmentId);
            appointmentCancellationEvent.put("caseId", caseId);
            appointmentCancellationEvent.put("doctorId", doctorId);
            appointmentCancellationEvent.put("patientId", patientId);
            appointmentCancellationEvent.put("appointmentTime", appointmentTime);
            appointmentCancellationEvent.put("reason", reason);
            appointmentCancellationEvent.put("timestamp", System.currentTimeMillis());

            // Send event to admin for refund
            kafkaTemplate.send("case-appointment-cancellation-topic", appointmentCancellationEvent);
            log.info("Appointment Cancellation event sent for case: {}, and patient {}",caseId,
                    patientId);

            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));
            // Send notification to patient about appointment cancellation
            NotificationDto patientNotification = NotificationDto.builder()
                    .senderUserId(doctor.getUserId())
                    .receiverUserId(getPatientUserId(caseId, doctor.getId() ))
                    .senderType(UserType.DOCTOR)
                    .receiverType(UserType.PATIENT)
                    .title("Appointment Cancellation")
                    .message("Doctor "+ doctorId +" has cancelled the appointment that was scheduled for "+
                            " your case: "+ caseId +", on: " + appointmentTime +
                            ". Reason: " + reason)
                    .type(NotificationType.APPOINTMENT)
                    .sendEmail(true)
                    .priority(NotificationPriority.CRITICAL)
                    .build();

            kafkaTemplate.send("notification-topic", patientNotification);
            log.info("Appointment Cancellation notification sent for case: {}, and patient {}",caseId,
                    patientId);

        } catch (Exception e) {
            log.error("Error sending Appointment Cancellation notification for case: {}: {}", caseId, e.getMessage(), e);
        }
    }

    public void sendCaseStatusUpdateEventFromDoctor(Long caseId, String oldStatus, String newStatus,
                                                    Long patientId, Long doctorId) {

        // Send case status update event
        Map<String, Object> caseEvent = new HashMap<>();
        caseEvent.put("caseId", caseId);
        caseEvent.put("oldStatus", oldStatus);
        caseEvent.put("newStatus", newStatus);
        caseEvent.put("patientId", patientId);
        caseEvent.put("doctorId", doctorId);
        caseEvent.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send("case-status-doctor-updated-topic", caseEvent);
        log.info("Kafka - Case status updated event sent for case: {}", caseId);
    }

    public void sendReportExportedEvent(Long caseId, String pdfUrl,Long doctorId, Long patientId, Long reportId){
        try {
            // Create case fee update event
            Map<String, Object> reportExportedEvent = new HashMap<>();
            reportExportedEvent.put("caseId", caseId);
            reportExportedEvent.put("pdfUrl", pdfUrl);
            reportExportedEvent.put("doctorId", doctorId);
            reportExportedEvent.put("patientId", patientId);
            reportExportedEvent.put("reportId", reportId);
            reportExportedEvent.put("timestamp", System.currentTimeMillis());

            // Send event to update case in patient service
            kafkaTemplate.send("case-report-update-topic", reportExportedEvent);
            log.info("Case medical report update event sent for case: {} with pdf URL: ${}", caseId, pdfUrl);

            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));
            // Send notification to patient about medical report being ready
            NotificationDto patientNotification = NotificationDto.builder()
                    .senderUserId(doctor.getUserId())
                    .receiverUserId(getPatientUserId(caseId, doctor.getId()))
                    .senderType(UserType.DOCTOR)
                    .receiverType(UserType.PATIENT)
                    .title("Medical Report is ready")
                    .message(String.format("Your doctor has finalized the medical report "+
                            " for your case: "+ caseId +
                            ", and you can download it using: "+ pdfUrl +" or by browsing your closed cases"))
                    .type(NotificationType.CONSULTATION)
                    .sendEmail(true)
                    .priority(NotificationPriority.MEDIUM)
                    .build();

            kafkaTemplate.send("notification-topic", patientNotification);
            log.info("Medical report notification sent to patient: {}", patientId);

        } catch (Exception e) {
            log.error("Error sending medical report readiness event for case {}: {}", caseId, e.getMessage(), e);
        }
    }

    /**
     * Send appointment reminder notification
     */
    public void sendAppointmentReminder(NotificationDto notification) {
        try {
            kafkaTemplate.send("notification-topic", notification);
            log.info("Appointment reminder sent to user: {}", notification.getReceiverId());
        } catch (Exception e) {
            log.error("Error sending appointment reminder: {}", e.getMessage(), e);
            throw e; // Rethrow to allow retry logic
        }
    }

    private Long getPatientUserId(Long caseId, Long doctorId) {
        Long patientId = null;
        try{
            patientId = patientServiceClient.getCustomPatientInfo( caseId, doctorId ).getBody().getData().getUserId();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return patientId;
    }
}