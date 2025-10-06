package com.doctorservice.scheduler;

import com.doctorservice.config.AppointmentReminderConfig;
import com.doctorservice.service.AppointmentReminderService;
import com.doctorservice.repository.AppointmentReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduler for processing appointment reminders
 * Runs periodically to check and send pending reminders
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderScheduler {

    private final AppointmentReminderService reminderService;
    private final AppointmentReminderRepository reminderRepository;
    private final AppointmentReminderConfig config;

    /**
     * Main reminder processing task
     * Runs every 5 minutes by default (configurable)
     */
    @Scheduled(fixedDelayString = "#{${appointment.reminders.check-interval-seconds:300} * 1000}")
    @Transactional
    public void processReminders() {
        if (!config.getEnabled()) {
            return;
        }

        log.debug("Starting reminder processing cycle");
        
        try {
            reminderService.processPendingReminders();
        } catch (Exception e) {
            log.error("Error processing reminders: {}", e.getMessage(), e);
        }
    }

    /**
     * Cleanup old reminders daily at 2 AM
     * Removes sent/failed/cancelled reminders older than 30 days
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldReminders() {
        log.info("Starting old reminders cleanup");
        
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
            reminderRepository.deleteOldReminders(cutoffDate);
            log.info("Completed old reminders cleanup");
        } catch (Exception e) {
            log.error("Error during reminders cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Log reminder statistics every hour
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void logReminderStats() {
        if (!config.getEnabled()) {
            return;
        }

        try {
            long totalReminders = reminderRepository.count();
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime oneHourAhead = now.plusHours(1);
            long upcomingReminders = reminderRepository
                    .findPendingRemindersInTimeWindow(now, oneHourAhead).size();
            
            log.info("Reminder Statistics - Total: {}, Upcoming (next hour): {}", 
                    totalReminders, upcomingReminders);
        } catch (Exception e) {
            log.error("Error logging reminder stats: {}", e.getMessage());
        }
    }

    /**
     * Handle overdue reminders - runs every 30 minutes
     * Marks old pending reminders as failed if they're significantly overdue
     */
    @Scheduled(fixedRate = 1800000) // Every 30 minutes
    @Transactional
    public void handleOverdueReminders() {
        if (!config.getEnabled()) {
            return;
        }

        try {
            // Consider reminders overdue if they're 30 minutes past scheduled time
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
            var overdueReminders = reminderRepository.findOverdueReminders(cutoff);
            
            if (!overdueReminders.isEmpty()) {
                log.warn("Found {} overdue reminders", overdueReminders.size());
                
                for (var reminder : overdueReminders) {
                    reminder.setStatus(com.doctorservice.entity.ReminderStatus.FAILED);
                    reminder.setFailureReason("Reminder became overdue and was not sent in time");
                    reminderRepository.save(reminder);
                }
            }
        } catch (Exception e) {
            log.error("Error handling overdue reminders: {}", e.getMessage(), e);
        }
    }
}