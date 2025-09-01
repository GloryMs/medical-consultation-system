package com.doctorservice.scheduler;

import com.doctorservice.config.WorkloadManagementConfig;
import com.doctorservice.service.DoctorWorkloadService;
import com.doctorservice.entity.Doctor;
import com.doctorservice.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkloadScheduledTasks {

    private final DoctorWorkloadService workloadService;
    private final DoctorRepository doctorRepository;
    private final WorkloadManagementConfig config;

    /**
     * Automatically recalculate workload for all doctors every 30 minutes
     */
    @Scheduled(fixedRateString = "#{${doctor.workload.auto-recalculation-interval-minutes:30} * 60000}")
    @Transactional
    public void autoRecalculateWorkloads() {
        if (!config.getAutoRecalculationEnabled()) {
            return;
        }

        log.info("Starting scheduled workload recalculation");
        
        try {
            workloadService.recalculateAllDoctorWorkloads();
            log.info("Scheduled workload recalculation completed successfully");
        } catch (Exception e) {
            log.error("Error in scheduled workload recalculation: {}", e.getMessage(), e);
        }
    }

    /**
     * Update today's appointment counts at the start of each day
     */
    @Scheduled(cron = "0 0 0 * * *") // Daily at midnight
    @Transactional
    public void resetDailyCounters() {
        log.info("Starting daily counter reset");
        
        try {
            List<Doctor> allDoctors = doctorRepository.findAll();
            
            for (Doctor doctor : allDoctors) {
                doctor.setTodayAppointments(0);
                // Recalculate will update with actual appointments for today
                workloadService.loadDoctorWorkload(doctor.getId());
            }
            
            log.info("Daily counter reset completed for {} doctors", allDoctors.size());
        } catch (Exception e) {
            log.error("Error in daily counter reset: {}", e.getMessage(), e);
        }
    }

    /**
     * Automatically disable emergency mode after configured hours
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    public void autoDisableEmergencyMode() {
        if (!config.getEmergencyModeAutoDisable()) {
            return;
        }

        log.debug("Checking for emergency modes to auto-disable");
        
        try {
            List<Doctor> emergencyDoctors = doctorRepository.findDoctorsInEmergencyMode();
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(config.getEmergencyModeMaxHours());
            
            int disabledCount = 0;
            for (Doctor doctor : emergencyDoctors) {
                if (doctor.getEmergencyModeEnabledAt() != null && 
                    doctor.getEmergencyModeEnabledAt().isBefore(cutoffTime)) {
                    
                    workloadService.disableEmergencyMode(doctor.getId());
                    disabledCount++;
                    
                    log.info("Auto-disabled emergency mode for doctor {} after {} hours", 
                            doctor.getId(), config.getEmergencyModeMaxHours());
                }
            }
            
            if (disabledCount > 0) {
                log.info("Auto-disabled emergency mode for {} doctors", disabledCount);
            }
        } catch (Exception e) {
            log.error("Error in emergency mode auto-disable: {}", e.getMessage(), e);
        }
    }

    /**
     * Clean up stale workload data and recalculate
     */
    @Scheduled(fixedRate = 7200000) // Every 2 hours
    @Transactional
    public void cleanupStaleWorkloadData() {
        log.debug("Checking for stale workload data");
        
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(config.getWorkloadStaleHours());
            List<Doctor> staleWorkloadDoctors = doctorRepository.findDoctorsWithOutdatedWorkload(cutoffTime);
            
            if (!staleWorkloadDoctors.isEmpty()) {
                log.info("Found {} doctors with stale workload data, recalculating", staleWorkloadDoctors.size());
                
                for (Doctor doctor : staleWorkloadDoctors) {
                    try {
                        workloadService.loadDoctorWorkload(doctor.getId());
                    } catch (Exception e) {
                        log.warn("Failed to recalculate workload for doctor {}: {}", 
                                doctor.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error in stale workload cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Monitor and alert on high system workload
     */
    @Scheduled(fixedRate = 1800000) // Every 30 minutes
    public void monitorSystemWorkload() {
        log.debug("Monitoring system workload");
        
        try {
            Double averageWorkload = doctorRepository.getAverageWorkloadPercentage();
            long availableDoctorCount = doctorRepository.countAvailableDoctorsForNewCases();
            
            if (averageWorkload != null && averageWorkload > 85.0) {
                log.warn("System workload is high: {}% average workload, {} available doctors", 
                        averageWorkload, availableDoctorCount);
                
                // Could trigger alerts, notifications, etc.
                // For now, just log the warning
            }
            
            if (availableDoctorCount < 5) {
                log.warn("Low doctor availability: only {} doctors available for new cases", 
                        availableDoctorCount);
            }
        } catch (Exception e) {
            log.error("Error in system workload monitoring: {}", e.getMessage(), e);
        }
    }

    /**
     * Generate workload statistics for reporting
     */
    @Scheduled(cron = "0 0 6 * * *") // Daily at 6 AM
    public void generateWorkloadStatistics() {
        log.info("Generating daily workload statistics");
        
        try {
            Double averageWorkload = doctorRepository.getAverageWorkloadPercentage();
            long totalDoctors = doctorRepository.count();
            long availableDoctors = doctorRepository.countAvailableDoctorsForNewCases();
            List<Doctor> overloadedDoctors = doctorRepository.findOverloadedDoctors();
            
            log.info("Daily Workload Statistics:");
            log.info("- Total Doctors: {}", totalDoctors);
            log.info("- Available for New Cases: {}", availableDoctors);
            log.info("- Average Workload: {}%", averageWorkload != null ? String.format("%.2f", averageWorkload) : "N/A");
            log.info("- Overloaded Doctors: {}", overloadedDoctors.size());
            
            // Could store these statistics in a database table for historical tracking
            
        } catch (Exception e) {
            log.error("Error generating workload statistics: {}", e.getMessage(), e);
        }
    }
}