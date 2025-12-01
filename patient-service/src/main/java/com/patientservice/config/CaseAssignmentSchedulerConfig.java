package com.patientservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

/**
 * Configuration properties for case assignment scheduling and reminder system
 * These properties control the automated case reassignment and doctor reminder workflows
 */
@Configuration
@ConfigurationProperties(prefix = "case.assignment")
@EnableScheduling
@Data
public class CaseAssignmentSchedulerConfig {

    /**
     * Enable/disable the case assignment expiration scheduler
     * Default: true
     */
    private Boolean schedulerEnabled = true;

    /**
     * How often to check for expired assignments (in seconds)
     * Default: 300 seconds (5 minutes)
     */
    private Integer checkIntervalSeconds = 300;

    /**
     * Timeout period in hours for doctor to accept case assignment
     * Default: 24 hours
     * After this period, unaccepted assignments will expire
     */
    private Integer assignmentTimeoutHours = 24;

    /**
     * Below timeout in hours is for critical and urgent cases
     */
    private Integer criticalAssignmentTimeoutHours = 4;

    /**
     * Whether to allow re-assignment of cases to the same doctor who didn't respond
     * Default: false
     * - If true: System can reassign expired cases to the same doctor
     * - If false: System will exclude the non-responsive doctor from reassignment
     */
    private Boolean canReassignToSameDoctor = false;

    /**
     * Enable/disable the reminder notification system
     * Default: true
     */
    private Boolean reminderEnabled = true;

    /**
     * Reminder times in hours before expiration
     * Default: [12, 20, 23] hours from assignment
     * This means reminders at 12h (12h remaining), 20h (4h remaining), 23h (1h remaining)
     */
    private List<Integer> reminderHoursFromAssignment = List.of(12, 20, 23);

    /**
     * Whether to send admin notifications for expired assignments
     * Default: true
     */
    private Boolean notifyAdminOnExpiration = true;

    /**
     * Maximum number of reassignment attempts for a single case
     * Default: 3
     * After this many expirations, the case requires manual intervention
     */
    private Integer maxReassignmentAttempts = 3;

    /**
     * Cooldown period in hours before reassigning to same doctor
     * Default: 24 hours (1 day)
     * Only applies if canReassignToSameDoctor is true
     */
    private Integer reassignmentCooldownHours = 24;

    /**
     * Whether to prioritize cases with multiple expirations
     * Default: true
     * Cases with more expirations get higher priority in reassignment
     */
    private Boolean prioritizeMultipleExpirations = true;

    /**
     * Grace period in minutes after expiration before triggering reassignment
     * Default: 5 minutes
     * Allows for system clock differences and processing delays
     */
    private Integer expirationGracePeriodMinutes = 5;

    /**
     * Calculate the exact timeout duration in hours
     */
    public long getAssignmentTimeoutInHours() {
        return assignmentTimeoutHours != null ? assignmentTimeoutHours : 24;
    }

    /**
     * Calculate reminder times in hours before expiration
     * Returns list sorted in ascending order (earliest reminder first)
     */
    public List<Integer> getReminderTimesBeforeExpiration() {
        if (reminderHoursFromAssignment == null || reminderHoursFromAssignment.isEmpty()) {
            return List.of(12, 20, 23); // Default reminders at 12h, 20h, 23h from assignment
        }
        return reminderHoursFromAssignment.stream()
                .filter(hours -> hours > 0 && hours < getAssignmentTimeoutInHours())
                .sorted()
                .toList();
    }

    /**
     * Get time remaining for each reminder (hours before expiration)
     */
    public List<Integer> getTimeRemainingForReminders() {
        return getReminderTimesBeforeExpiration().stream()
                .map(hoursFromStart -> (int)(getAssignmentTimeoutInHours() - hoursFromStart))
                .toList();
    }
}