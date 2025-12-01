package com.patientservice.scheduler;

import com.commonlibrary.entity.AssignmentStatus;
import com.commonlibrary.entity.CaseStatus;
import com.patientservice.config.CaseAssignmentSchedulerConfig;
import com.patientservice.entity.Case;
import com.patientservice.entity.CaseAssignment;
import com.patientservice.entity.CaseAssignmentReminder;
import com.patientservice.kafka.PatientEventProducer;
import com.patientservice.repository.CaseAssignmentReminderRepository;
import com.patientservice.repository.CaseAssignmentRepository;
import com.patientservice.repository.CaseRepository;
import com.patientservice.service.SmartCaseAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scheduler service for handling expired case assignments and sending reminder notifications
 * Responsibilities:
 * 1. Monitor pending assignments and expire them after configured timeout period
 * 2. Send reminder notifications to doctors at configured intervals
 * 3. Trigger case reassignment for expired assignments
 * 4. Notify admins about expired assignments
 * 5. Track reassignment attempts and prevent infinite loops
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CaseAssignmentScheduler {

    private final CaseAssignmentRepository caseAssignmentRepository;
    private final CaseRepository caseRepository;
    private final CaseAssignmentReminderRepository reminderRepository;
    //private final SmartCaseAssignmentService smartCaseAssignmentService;
    private final PatientEventProducer patientEventProducer;
    private final CaseAssignmentSchedulerConfig config;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    /**
     * Main scheduler: Handle expired assignments and trigger reassignment
     * Runs every 5 minutes (configurable via check-interval-seconds)
     * Process:
     * 1. Find all PENDING assignments past expiration time
     * 2. Mark assignments as EXPIRED
     * 3. Update case status to PENDING if needed
     * 4. Send notifications to admin
     * 5. Trigger SmartCaseAssignmentService for reassignment (respecting doctor exclusion rules)
     */
    @Scheduled(fixedDelayString = "${case.assignment.check-interval-seconds:300}000")
    @Transactional
    public void handleExpiredAssignments() {

        log.info("CaseAssignmentScheduler-handleExpiredAssignments:");

        if (!config.getSchedulerEnabled()) {
            log.debug("Case assignment scheduler is disabled");
            return;
        }

        try {
            log.info("====> Handle expired assignments scheduler has been started");
            LocalDateTime cutoffTime = LocalDateTime.now()
                    .minusMinutes(config.getExpirationGracePeriodMinutes());

            // Find all PENDING assignments that have expired
            List<CaseAssignment> expiredAssignments = caseAssignmentRepository
                    .findByStatusAndExpiresAtBefore(AssignmentStatus.PENDING, cutoffTime);

            if (expiredAssignments.isEmpty()) {
                log.debug("No expired assignments found");
                return;
            }

            log.info("Found {} expired case assignments to process", expiredAssignments.size());

            for (CaseAssignment assignment : expiredAssignments) {
                processExpiredAssignment(assignment);
            }

            log.info("Successfully processed {} expired assignments", expiredAssignments.size());

        } catch (Exception e) {
            log.error("Error in expired assignment scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Process a single expired assignment
     */
    private void processExpiredAssignment(CaseAssignment assignment) {
        try {
            Case medicalCase = assignment.getCaseEntity();
            Long doctorId = assignment.getDoctorId();

            log.info("Processing expired assignment {} for case {} assigned to doctor {}",
                    assignment.getId(), medicalCase.getId(), doctorId);

            // Update assignment status to EXPIRED
            assignment.setStatus(AssignmentStatus.EXPIRED);
            assignment.setRespondedAt(LocalDateTime.now());
            caseAssignmentRepository.save(assignment);

            log.info("Updating assignment {} status to EXPIRED successfully" , assignment.getId());

            // Update case status back to PENDING for reassignment
            if (medicalCase.getStatus() == CaseStatus.ASSIGNED) {
                medicalCase.setStatus(CaseStatus.PENDING);
                caseRepository.save(medicalCase);
                log.info("Updated case {} status back to PENDING", medicalCase.getId());
            }

            // Send notification to admin
            log.info("Send notification to admin");
            if (config.getNotifyAdminOnExpiration()) {
                sendAdminNotification(assignment, medicalCase, doctorId);
            }


            // Check reassignment attempt limits
            log.info("Check reassignment attempt limits");
            long count = 0;
            try{
                count = caseAssignmentRepository
                        .countByCaseEntityIdAndStatus(medicalCase.getId(), AssignmentStatus.EXPIRED);
            }catch (Exception e) {
                System.out.println("Here is the exception from parsing the count of expirations");
                System.out.println(e.getMessage());
                e.printStackTrace();
            }

            System.out.println("count of expiration temp val is: " + count);
            int expirationCount = Long.valueOf(count).intValue();
            log.info("expirationCount for assignment {}, is {}", assignment.getId(), expirationCount);

            if (expirationCount >= config.getMaxReassignmentAttempts()) {
                log.warn("Case {} has reached maximum reassignment attempts ({}). Manual intervention required.",
                        medicalCase.getId(), config.getMaxReassignmentAttempts());

                log.info("Send Admin Escalation Notification");
                sendAdminEscalationNotification(medicalCase, expirationCount);

                return;
            }

            // Determine doctors to exclude from reassignment
            log.info("Determine doctors to exclude from reassignment");
            Set<Long> excludedDoctorIds = determineExcludedDoctors(medicalCase, doctorId);

            // Trigger SmartCaseAssignmentService for reassignment
            log.info("Trigger SmartCaseAssignmentService for reassignment");
            triggerCaseReassignment(medicalCase, excludedDoctorIds);

        } catch (Exception e) {
            log.error("Error processing expired assignment {}: {}", 
                    assignment.getId(), e.getMessage(), e);
            e.printStackTrace();
        }
    }

    /**
     * Determine which doctors should be excluded from reassignment
     */
    private Set<Long> determineExcludedDoctors(Case medicalCase, Long expiredDoctorId) {
        Set<Long> excludedDoctors = new java.util.HashSet<>();

        if (!config.getCanReassignToSameDoctor()) {
            // Exclude all doctors who have expired assignments for this case
            List<CaseAssignment> expiredAssignments = caseAssignmentRepository
                    .findByCaseEntityIdAndStatus(medicalCase.getId(), AssignmentStatus.EXPIRED);
            
            excludedDoctors = expiredAssignments.stream()
                    .map(CaseAssignment::getDoctorId)
                    .collect(Collectors.toSet());

            log.info("Excluding {} doctors from reassignment for case {}", 
                    excludedDoctors.size(), medicalCase.getId());
        } else {
            // Check cooldown period for same doctor
            LocalDateTime cooldownCutoff = LocalDateTime.now()
                    .minusHours(config.getReassignmentCooldownHours());

            List<CaseAssignment> recentExpiredAssignments = caseAssignmentRepository
                    .findByCaseEntityIdAndDoctorIdAndStatusAndRespondedAtAfter(
                            medicalCase.getId(), 
                            expiredDoctorId,
                            AssignmentStatus.EXPIRED,
                            cooldownCutoff
                    );

            if (!recentExpiredAssignments.isEmpty()) {
                excludedDoctors.add(expiredDoctorId);
                log.info("Doctor {} is in cooldown period for case {}", 
                        expiredDoctorId, medicalCase.getId());
            }
        }

        return excludedDoctors;
    }

    /**
     * Trigger case reassignment through SmartCaseAssignmentService
     */
    private void triggerCaseReassignment(Case medicalCase, Set<Long> excludedDoctorIds) {
        try {
            log.info("Triggering reassignment for case {} (excluding {} doctors)", 
                    medicalCase.getId(), excludedDoctorIds.size());

            // Send Kafka event to trigger SmartCaseAssignmentService
            // The service will handle the actual reassignment logic
            patientEventProducer.sendStartSmartCaseAssignmentService(
                    medicalCase.getId(), 
                    excludedDoctorIds
            );

            log.info("Successfully triggered reassignment for case {}", medicalCase.getId());

        } catch (Exception e) {
            log.error("Failed to trigger reassignment for case {}: {}", 
                    medicalCase.getId(), e.getMessage(), e);
        }
    }

    /**
     * Send notification to admin about expired assignment
     */
    private void sendAdminNotification(CaseAssignment assignment, Case medicalCase, Long doctorId) {
        try {
            String title = "Case Assignment Expired";
            String message = String.format(
                    "Case #%d ('%s') assignment to Doctor #%d has expired without response. " +
                    "Assignment was created at %s and expired at %s. " +
                    "The case is being automatically reassigned to another doctor.",
                    medicalCase.getId(),
                    medicalCase.getCaseTitle(),
                    doctorId,
                    assignment.getAssignedAt().format(TIME_FORMATTER),
                    assignment.getExpiresAt().format(TIME_FORMATTER)
            );

            patientEventProducer.sendAdminNotification(title, message, "CASE_ASSIGNMENT_EXPIRED");
            log.info("Sent admin notification for expired assignment {}", assignment.getId());

        } catch (Exception e) {
            log.error("Failed to send admin notification for assignment {}: {}", 
                    assignment.getId(), e.getMessage());
        }
    }

    /**
     * Send escalation notification when max attempts reached
     */
    private void sendAdminEscalationNotification(Case medicalCase, int expirationCount) {
        try {
            String title = "URGENT: Case Assignment Requires Manual Intervention";
            String message = String.format(
                    "Case #%d ('%s') has reached maximum reassignment attempts (%d). " +
                    "Multiple doctors have not responded to this case assignment. " +
                    "Manual review and intervention is required. " +
                    "Case details: Urgency=%s, Specialization=%s, Created=%s",
                    medicalCase.getId(),
                    medicalCase.getCaseTitle(),
                    expirationCount,
                    medicalCase.getUrgencyLevel(),
                    medicalCase.getRequiredSpecialization(),
                    medicalCase.getCreatedAt().format(TIME_FORMATTER)
            );

            patientEventProducer.sendAdminNotification(title, message, "CASE_ASSIGNMENT_ESCALATION");
            log.warn("Sent escalation notification for case {} after {} expirations", 
                    medicalCase.getId(), expirationCount);

        } catch (Exception e) {
            log.error("Failed to send escalation notification for case {}: {}", 
                    medicalCase.getId(), e.getMessage());
        }
    }

    /**
     * Reminder scheduler: Send reminder notifications to doctors
     * Runs every 5 minutes to check for pending reminders
     * 
     * Process:
     * 1. Find all PENDING assignments that need reminders
     * 2. Check reminder schedule (12h, 20h, 23h from assignment)
     * 3. Create reminder records to prevent duplicate notifications
     * 4. Send notifications to doctors with time remaining
     */
    @Scheduled(fixedDelayString = "${case.assignment.check-interval-seconds:300}000")
    @Transactional
    public void sendAssignmentReminders() {
        if (!config.getReminderEnabled()) {
            log.debug("Case assignment reminder system is disabled");
            return;
        }

        try {
            log.info("====> Send assignment reminders scheduler has been started");
            // Get reminder times (hours from assignment start)
            List<Integer> reminderHours = config.getReminderTimesBeforeExpiration();
            
            if (reminderHours.isEmpty()) {
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            
            for (Integer hoursFromAssignment : reminderHours) {
                processRemindersForTimepoint(now, hoursFromAssignment);
            }

        } catch (Exception e) {
            log.error("Error in assignment reminder scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Process reminders for a specific timepoint
     */
    private void processRemindersForTimepoint(LocalDateTime now, Integer hoursFromAssignment) {
        try {
            // Calculate the time window for this reminder
            // E.g., for 12h reminder, find assignments created 12h ± 2.5min ago
            log.info("Processing Reminders for Case Assignment");
            log.info("Time now: {}", LocalDateTime.now().format(TIME_FORMATTER) );
            log.info("Hours from assignment time: {}", hoursFromAssignment);
            LocalDateTime targetTime = now.minusHours(hoursFromAssignment);
            LocalDateTime windowStart = targetTime.minusMinutes(config.getCheckIntervalSeconds() / 60);
            LocalDateTime windowEnd = targetTime.plusMinutes(config.getCheckIntervalSeconds() / 60);

            // Find PENDING assignments in this time window
            log.info("Searching the pending (assigned) assignment between {} and {} ", windowStart, windowEnd);
            List<CaseAssignment> assignmentsNeedingReminder = caseAssignmentRepository
                    .findByStatusAndAssignedAtBetween(AssignmentStatus.PENDING, windowStart, windowEnd);

            if (assignmentsNeedingReminder.isEmpty()) {
                return;
            }

            log.info("Found {} assignments needing {}-hour reminder", 
                    assignmentsNeedingReminder.size(), hoursFromAssignment);

            for (CaseAssignment assignment : assignmentsNeedingReminder) {
                sendReminderNotification(assignment, hoursFromAssignment);
            }

        } catch (Exception e) {
            log.error("Error processing reminders for {}-hour timepoint: {}", 
                    hoursFromAssignment, e.getMessage(), e);
        }
    }

    /**
     * Send reminder notification to doctor
     */
    private void sendReminderNotification(CaseAssignment assignment, Integer hoursFromAssignment) {
        try {
            // Check if reminder already sent for this timepoint
            boolean reminderExists = reminderRepository.existsByAssignmentAndReminderHour(
                    assignment, hoursFromAssignment);

            if (reminderExists) {
                log.debug("Reminder already sent for assignment {} at {} hours", 
                        assignment.getId(), hoursFromAssignment);
                return;
            }

            Case medicalCase = assignment.getCaseEntity();
            Long doctorId = assignment.getDoctorId();

            // Calculate time remaining until expiration
            long timeoutHours = config.getAssignmentTimeoutInHours();
            long hoursRemaining = timeoutHours - hoursFromAssignment;
            String timeRemainingText = formatTimeRemaining(hoursRemaining);

            // Build reminder message
            String title = "⏰ Case Assignment Reminder";
            String message = String.format(
                    "REMINDER: You have %s to accept the case assignment.\n\n" +
                    "📋 Case: %s\n" +
                    "🔴 Urgency: %s\n" +
                    "📅 Assigned: %s\n" +
                    "⏳ Expires: %s\n\n" +
                    "⚠️ If not accepted, this case will be reassigned to another doctor.\n\n" +
                    "Please review and respond as soon as possible.",
                    timeRemainingText,
                    medicalCase.getCaseTitle(),
                    medicalCase.getUrgencyLevel(),
                    assignment.getAssignedAt().format(TIME_FORMATTER),
                    assignment.getExpiresAt().format(TIME_FORMATTER)
            );

            // Send notification via Kafka
            patientEventProducer.sendAssignmentReminderNotification(
                    doctorId,
                    medicalCase.getId(),
                    assignment.getId(),
                    title,
                    message
            );

            // Create reminder record to prevent duplicates
            CaseAssignmentReminder reminder = CaseAssignmentReminder.builder()
                    .assignment(assignment)
                    .reminderHour(hoursFromAssignment)
                    .sentAt(LocalDateTime.now())
                    .hoursRemaining(hoursRemaining)
                    .build();
            reminderRepository.save(reminder);

            log.info("Sent {}-hour reminder for assignment {} to doctor {}", 
                    hoursFromAssignment, assignment.getId(), doctorId);

        } catch (Exception e) {
            log.error("Failed to send reminder for assignment {}: {}", 
                    assignment.getId(), e.getMessage(), e);
        }
    }

    /**
     * Format time remaining for display
     */
    private String formatTimeRemaining(long hours) {
        if (hours <= 1) {
            return hours + " hour";
        } else if (hours < 24) {
            return hours + " hours";
        } else {
            long days = hours / 24;
            long remainingHours = hours % 24;
            if (remainingHours == 0) {
                return days + (days == 1 ? " day" : " days");
            } else {
                return String.format("%d %s and %d %s", 
                        days, days == 1 ? "day" : "days",
                        remainingHours, remainingHours == 1 ? "hour" : "hours");
            }
        }
    }

    /**
     * Cleanup old reminder records - runs daily
     * Removes reminder records older than 30 days
     */
    @Scheduled(cron = "0 0 2 * * *") // Run at 2 AM daily
    @Transactional
    public void cleanupOldReminders() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
            int deletedCount = reminderRepository.deleteBysentAtBefore(cutoffDate);
            
            if (deletedCount > 0) {
                log.info("Cleaned up {} old reminder records", deletedCount);
            }
        } catch (Exception e) {
            log.error("Error cleaning up old reminders: {}", e.getMessage(), e);
        }
    }

    /**
     * Get scheduler statistics - useful for monitoring
     */
    public SchedulerStatistics getSchedulerStatistics() {
        try {
            long totalPending = caseAssignmentRepository.countByStatus(AssignmentStatus.PENDING);
            long totalExpired = caseAssignmentRepository.countByStatus(AssignmentStatus.EXPIRED);
            long casesRequiringIntervention = caseRepository.countCasesWithMultipleExpirations(
                    config.getMaxReassignmentAttempts());

            LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
            long expiredLast24h = caseAssignmentRepository.countByStatusAndRespondedAtAfter(
                    AssignmentStatus.EXPIRED, oneDayAgo);

            long remindersSentToday = reminderRepository.countBysentAtAfter(
                    LocalDateTime.now().withHour(0).withMinute(0).withSecond(0));

            return SchedulerStatistics.builder()
                    .totalPendingAssignments(totalPending)
                    .totalExpiredAssignments(totalExpired)
                    .expiredLast24Hours(expiredLast24h)
                    .casesRequiringManualIntervention(casesRequiringIntervention)
                    .remindersSentToday(remindersSentToday)
                    .schedulerEnabled(config.getSchedulerEnabled())
                    .reminderEnabled(config.getReminderEnabled())
                    .assignmentTimeoutHours(config.getAssignmentTimeoutInHours())
                    .build();

        } catch (Exception e) {
            log.error("Error getting scheduler statistics: {}", e.getMessage());
            return SchedulerStatistics.builder().build();
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class SchedulerStatistics {
        private Long totalPendingAssignments;
        private Long totalExpiredAssignments;
        private Long expiredLast24Hours;
        private Long casesRequiringManualIntervention;
        private Long remindersSentToday;
        private Boolean schedulerEnabled;
        private Boolean reminderEnabled;
        private Long assignmentTimeoutHours;
    }
}