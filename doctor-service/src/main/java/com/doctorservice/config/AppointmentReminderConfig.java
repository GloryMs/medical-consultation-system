package com.doctorservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "appointment.reminders")
@Data
public class AppointmentReminderConfig {

    /**
     * Enable/disable appointment reminder system
     */
    private Boolean enabled = true;

    /**
     * Default reminder times in minutes before appointment
     * Example: [1440, 60, 15] = 24 hours, 1 hour, 15 minutes before
     */
    private List<Integer> defaultReminderMinutes = List.of(1440, 60, 15);

    /**
     * Configurable reminder times for doctors (in minutes)
     */
    private List<Integer> doctorReminderMinutes = List.of(1440, 120, 30);

    /**
     * Configurable reminder times for patients (in minutes)
     */
    private List<Integer> patientReminderMinutes = List.of(1440, 60, 15);

    /**
     * How often to check for pending reminders (in seconds)
     */
    private Integer checkIntervalSeconds = 300; // Every 5 minutes

    /**
     * Send reminders only for these appointment statuses
     */
    private List<String> eligibleStatuses = List.of("CONFIRMED");

    /**
     * Maximum retry attempts for failed reminders
     */
    private Integer maxRetryAttempts = 3;

    /**
     * Include meeting link in reminder
     */
    private Boolean includeMeetingLink = true;

    /**
     * Time window (minutes) to send reminder before exact scheduled time
     * Prevents sending too early if scheduler runs ahead
     */
    private Integer sendWindowMinutes = 10;
}