package com.doctorservice.service;

import com.doctorservice.config.AppointmentReminderConfig;
import com.doctorservice.entity.Appointment;
import com.doctorservice.entity.AppointmentReminder;
import com.doctorservice.entity.AppointmentReminder.RecipientType;
import com.doctorservice.entity.ReminderStatus;
import com.doctorservice.entity.Doctor;
import com.doctorservice.kafka.DoctorEventProducer;
import com.doctorservice.repository.AppointmentReminderRepository;
import com.doctorservice.repository.AppointmentRepository;
import com.doctorservice.repository.DoctorRepository;
import com.commonlibrary.dto.NotificationDto;
import com.commonlibrary.entity.AppointmentStatus;
import com.commonlibrary.entity.NotificationPriority;
import com.commonlibrary.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderService {

    private final AppointmentReminderRepository reminderRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorEventProducer eventProducer;
    private final AppointmentReminderConfig config;

    /**
     * Create reminders when appointment is confirmed
     */
    @Transactional
    public void createRemindersForAppointment(Long appointmentId) {
        if (!config.getEnabled()) {
            log.debug("Reminder system is disabled");
            return;
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        // Only create reminders for CONFIRMED appointments
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            log.debug("Appointment {} is not confirmed, skipping reminder creation", appointmentId);
            return;
        }

        // Get doctor timezone
        Doctor doctor = appointment.getDoctor();
        String doctorTimezone = getDoctorTimezone(doctor);

        // Get patient timezone from patient service or use default
        Long patientId = appointment.getPatientId();
        String patientTimezone = getPatientTimezone(patientId);

        // Create reminders for doctor
        createRemindersForRecipient(
            appointment, 
            doctor.getId(),
            RecipientType.DOCTOR, 
            doctorTimezone,
            config.getDoctorReminderMinutes()
        );

        // Create reminders for patient
        createRemindersForRecipient(
            appointment, patientId,
            RecipientType.PATIENT, 
            patientTimezone,
            config.getPatientReminderMinutes()
        );

        log.info("Created reminders for appointment {} (Doctor timezone: {}, Patient timezone: {})", 
                appointmentId, doctorTimezone, patientTimezone);
    }

    /**
     * Create reminders for a specific recipient
     */
    private void createRemindersForRecipient(
            Appointment appointment,
            Long recipientUserId,
            RecipientType recipientType,
            String recipientTimezone,
            List<Integer> reminderMinutes) {

        LocalDateTime appointmentTime = appointment.getScheduledTime();
        ZoneId recipientZone = ZoneId.of(recipientTimezone);

        for (Integer minutesBefore : reminderMinutes) {
            // Calculate when to send reminder in UTC
            LocalDateTime scheduledSendTime = appointmentTime.minusMinutes(minutesBefore);

            // Skip if reminder time is in the past
            if (scheduledSendTime.isBefore(LocalDateTime.now())) {
                log.debug("Skipping past reminder: {} minutes before for {}", 
                         minutesBefore, recipientType);
                continue;
            }

            // Convert appointment time to recipient's timezone
            ZonedDateTime appointmentInRecipientTz = appointmentTime
                    .atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(recipientZone);

            AppointmentReminder reminder = AppointmentReminder.builder()
                    .appointment(appointment)
                    .recipientUserId(recipientUserId)
                    .recipientType(recipientType)
                    .minutesBefore(minutesBefore)
                    .scheduledSendTime(scheduledSendTime)
                    .recipientLocalTime(appointmentInRecipientTz.toLocalDateTime())
                    .recipientTimezone(recipientTimezone)
                    .status(ReminderStatus.PENDING)
                    .emailSent(false)
                    .inAppNotificationSent(false)
                    .build();

            reminderRepository.save(reminder);
            log.debug("Created reminder: {} for {} {} minutes before appointment", 
                     reminder.getId(), recipientType, minutesBefore);
        }
    }

    /**
     * Process pending reminders - called by scheduler
     */
    @Transactional
    public void processPendingReminders() {

        LocalDateTime now = LocalDateTime.now();
        //TODO must remove +3 and correct server time
        now = now.plusHours(3);
        LocalDateTime windowEnd = now.plusMinutes(config.getSendWindowMinutes());
        //TODO must remove +3 and correct server time
        windowEnd = windowEnd.plusHours(3);

        System.out.println("Reminder Window Start: " + now);
        System.out.println("Reminder Window end: " + windowEnd);

        List<AppointmentReminder> pendingReminders = reminderRepository
                .findPendingRemindersInTimeWindow(now, windowEnd);

        log.info("Processing {} pending reminders", pendingReminders.size());

        for (AppointmentReminder reminder : pendingReminders) {
            try {
                sendReminder(reminder);
            } catch (Exception e) {
                log.error("Failed to send reminder {}: {}", reminder.getId(), e.getMessage(), e);
                handleReminderFailure(reminder, e.getMessage());
            }
        }
    }

    /**
     * Send a single reminder
     */
    private void sendReminder(AppointmentReminder reminder) {
        Appointment appointment = reminder.getAppointment();

        // Double-check appointment is still confirmed
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            log.info("Appointment {} no longer confirmed, cancelling reminder {}", 
                    appointment.getId(), reminder.getId());
            reminder.setStatus(ReminderStatus.CANCELLED);
            reminderRepository.save(reminder);
            return;
        }

        String recipientType = reminder.getRecipientType().name();
        String timeUntil = formatTimeUntil(reminder.getMinutesBefore());
        
        // Format appointment time in recipient's timezone
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");
        String formattedTime = reminder.getRecipientLocalTime().format(formatter);
        String timezone = getTimezoneAbbreviation(reminder.getRecipientTimezone());

        // Build notification message
        StringBuilder message = new StringBuilder();
        message.append(String.format("Reminder: Your appointment is in %s\n\n", timeUntil));
        message.append(String.format("📅 %s (%s)\n", formattedTime, timezone));
        message.append(String.format("⏱️ Duration: %d minutes\n", appointment.getDuration()));
        message.append(String.format("💼 Consultation Type: %s\n", appointment.getConsultationType()));

        if (config.getIncludeMeetingLink() && appointment.getMeetingLink() != null) {
            message.append(String.format("\n🔗 Meeting Link: %s\n", appointment.getMeetingLink()));
            if (appointment.getMeetingId() != null) {
                message.append(String.format("🆔 Meeting ID: %s\n", appointment.getMeetingId()));
            }
        }

        if (reminder.getRecipientType() == RecipientType.PATIENT) {
            message.append(String.format("\n👨‍⚕️ Doctor: Dr. %s\n", appointment.getDoctor().getFullName()));
        } else {
            message.append(String.format("\n👤 Patient ID: %d\n", appointment.getPatientId()));
        }

        message.append("\nPlease be ready 5 minutes before the scheduled time.");

        // Send via Kafka
        NotificationDto notification = NotificationDto.builder()
                .senderId(0L) // System
                .receiverId(reminder.getRecipientUserId())
                .title(String.format("Appointment Reminder - %s", timeUntil))
                .message(message.toString())
                .type(NotificationType.APPOINTMENT)
                .priority(getNotificationPriority(reminder.getMinutesBefore()))
                .sendEmail(true)
                .build();

        eventProducer.sendAppointmentReminder(notification);

        // Update reminder status
        reminder.setStatus(ReminderStatus.SENT);
        reminder.setSentAt(LocalDateTime.now());
        reminder.setEmailSent(true);
        reminder.setInAppNotificationSent(true);
        reminderRepository.save(reminder);

        log.info("Sent reminder {} for appointment {} to {} ({})", 
                reminder.getId(), appointment.getId(), recipientType, timeUntil);
    }

    /**
     * Handle reminder sending failure
     */
    private void handleReminderFailure(AppointmentReminder reminder, String reason) {
        reminder.setStatus(ReminderStatus.FAILED);
        reminder.setFailureReason(reason);
        reminderRepository.save(reminder);
    }

    /**
     * Cancel all reminders for an appointment
     */
    @Transactional
    public void cancelRemindersForAppointment(Long appointmentId) {
        List<AppointmentReminder> reminders = reminderRepository
                .findByAppointmentIdAndStatus(appointmentId, ReminderStatus.PENDING);
        
        for (AppointmentReminder reminder : reminders) {
            reminder.setStatus(ReminderStatus.CANCELLED);
            reminderRepository.save(reminder);
        }
        
        log.info("Cancelled {} reminders for appointment {}", reminders.size(), appointmentId);
    }

    /**
     * Get doctor timezone from doctor entity or default to UTC
     */
    private String getDoctorTimezone(Doctor doctor) {
        // Priority: Doctor's configured timezone > Country-based timezone > UTC
        if (doctor.getCountry() != null) {
            return getTimezoneForCountry(doctor.getCountry());
        }
        return "UTC";
    }

    /**
     * Get patient timezone - would call patient service
     */
    private String getPatientTimezone(Long patientId) {
        // TODO: Call patient service to get timezone
        // For now, return UTC as default
        return "UTC";
    }

    /**
     * Map country to timezone
     */
    private String getTimezoneForCountry(String country) {
        // This is a simplified mapping - in production, use a proper library
        return switch (country.toUpperCase()) {
            case "GERMANY", "DE" -> "Europe/Berlin";
            case "USA", "US" -> "America/New_York";
            case "UK", "GB" -> "Europe/London";
            case "FRANCE", "FR" -> "Europe/Paris";
            case "SPAIN", "ES" -> "Europe/Madrid";
            case "ITALY", "IT" -> "Europe/Rome";
            case "CHINA", "CN" -> "Asia/Shanghai";
            case "JAPAN", "JP" -> "Asia/Tokyo";
            case "INDIA", "IN" -> "Asia/Kolkata";
            case "AUSTRALIA", "AU" -> "Australia/Sydney";
            case "BRAZIL", "BR" -> "America/Sao_Paulo";
            case "CANADA", "CA" -> "America/Toronto";
            default -> "UTC";
        };
    }

    /**
     * Get timezone abbreviation
     */
    private String getTimezoneAbbreviation(String timezoneId) {
        ZoneId zone = ZoneId.of(timezoneId);
        return zone.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH);
    }

    /**
     * Format time until appointment
     */
    private String formatTimeUntil(Integer minutes) {
        if (minutes >= 1440) {
            int days = minutes / 1440;
            return days + (days == 1 ? " day" : " days");
        } else if (minutes >= 60) {
            int hours = minutes / 60;
            return hours + (hours == 1 ? " hour" : " hours");
        } else {
            return minutes + " minutes";
        }
    }

    /**
     * Determine notification priority based on time until appointment
     */
    private NotificationPriority getNotificationPriority(Integer minutesBefore) {
        if (minutesBefore <= 30) {
            return NotificationPriority.CRITICAL;
        } else if (minutesBefore <= 60) {
            return NotificationPriority.HIGH;
        } else if (minutesBefore <= 120) {
            return NotificationPriority.MEDIUM;
        } else {
            return NotificationPriority.LOW;
        }
    }
}