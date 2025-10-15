package com.authservice.service;

import com.authservice.entity.PasswordResetCode;
import com.authservice.repository.PasswordResetCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for scheduled tasks related to authentication
 * Currently handles cleanup of expired password reset codes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {

    private final PasswordResetCodeRepository resetCodeRepository;

    private static final int MAX_ATTEMPTS = 3;

    /**
     * Clean up expired password reset codes every hour
     * Cron expression: second minute hour day month weekday
     * "0 0 * * * *" = at the start of every hour
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredResetCodes() {
        try {
            log.info("Starting cleanup of expired password reset codes");

            LocalDateTime now = LocalDateTime.now();

            // Count codes before deletion for logging
            long expiredCount = resetCodeRepository.findAll().stream()
                    .filter(code -> code.getExpiryTime().isBefore(now))
                    .count();

            // Delete expired codes
            resetCodeRepository.deleteByExpiryTimeBefore(now);

            log.info("Cleaned up {} expired password reset codes", expiredCount);

        } catch (Exception e) {
            log.error("Error cleaning up expired reset codes", e);
        }
    }

    /**
     * Clean up codes that have reached maximum attempts
     * Runs every 30 minutes
     * "0 30 * * * *" = every 30 minutes*/

    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void cleanupMaxAttemptsReachedCodes() {
        try {
            log.info("Starting cleanup of codes with max attempts reached");

            // Find codes with max attempts and mark them as used
            List<PasswordResetCode> maxAttemptCodes =
                    resetCodeRepository.findCodesWithMaxAttempts(MAX_ATTEMPTS);

            for (PasswordResetCode code : maxAttemptCodes) {
                code.setIsUsed(true);
            }

            if (!maxAttemptCodes.isEmpty()) {
                resetCodeRepository.saveAll(maxAttemptCodes);
                log.info("Marked {} codes as used due to max attempts reached",
                        maxAttemptCodes.size());
            }

        } catch (Exception e) {
            log.error("Error cleaning up max attempt codes", e);
        }
    }

    /**
     * Log statistics about password reset codes
     * Runs daily at midnight
     * "0 0 0 * * *" = every day at midnight
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void logPasswordResetStatistics() {
        try {
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            LocalDateTime lastWeek = LocalDateTime.now().minusDays(7);

            long codesLastDay = resetCodeRepository.countCodesSince(yesterday);
            long codesLastWeek = resetCodeRepository.countCodesSince(lastWeek);
            long successfulLastDay = resetCodeRepository.countSuccessfulResetsSince(yesterday);
            long successfulLastWeek = resetCodeRepository.countSuccessfulResetsSince(lastWeek);

            log.info("Password Reset Statistics:");
            log.info("  Codes generated last 24h: {}", codesLastDay);
            log.info("  Codes generated last 7 days: {}", codesLastWeek);
            log.info("  Successful resets last 24h: {}", successfulLastDay);
            log.info("  Successful resets last 7 days: {}", successfulLastWeek);

            if (codesLastDay > 0) {
                double successRate = (successfulLastDay * 100.0) / codesLastDay;
                log.info("  Success rate last 24h: {:.2f}%", successRate);
            }

        } catch (Exception e) {
            log.error("Error logging password reset statistics", e);
        }
    }

    /**
     * Warn about codes expiring soon (optional - for monitoring)
     * Runs every 5 minutes
     * "0 *5 * * * *" = every 5 minutes*/
//    @Scheduled(cron = "0 */5 * * * *")
//    public void checkCodesExpiringSoon() {
//        try {
//            LocalDateTime now = LocalDateTime.now();
//            LocalDateTime twoMinutesFromNow = now.plusMinutes(2);
//
//            List<PasswordResetCode> expiringSoon =
//                    resetCodeRepository.findCodesExpiringSoon(now, twoMinutesFromNow);
//
//            if (!expiringSoon.isEmpty()) {
//                log.debug("{} password reset codes will expire in the next 2 minutes",
//                        expiringSoon.size());
//            }
//
//        } catch (Exception e) {
//            log.error("Error checking codes expiring soon", e);
//        }
//    }
}