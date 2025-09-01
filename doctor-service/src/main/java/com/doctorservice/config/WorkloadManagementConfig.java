package com.doctorservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "doctor.workload")
@EnableScheduling
@Data
public class WorkloadManagementConfig {

    /**
     * Maximum number of active cases per doctor
     */
    private Integer maxActiveCases = 10;

    /**
     * Maximum daily appointments per doctor
     */
    private Integer maxDailyAppointments = 8;

    /**
     * Buffer time in minutes between appointments
     */
    private Integer bufferMinutes = 15;

    /**
     * Workload percentage threshold for auto-disable
     */
    private Double autoDisableThreshold = 95.0;

    /**
     * Hours before workload data is considered stale
     */
    private Integer workloadStaleHours = 2;

    /**
     * Enable automatic workload recalculation
     */
    private Boolean autoRecalculationEnabled = true;

    /**
     * Interval for automatic workload recalculation in minutes
     */
    private Integer autoRecalculationIntervalMinutes = 30;

    /**
     * Enable emergency mode auto-disable after hours
     */
    private Boolean emergencyModeAutoDisable = true;

    /**
     * Hours after which emergency mode is automatically disabled
     */
    private Integer emergencyModeMaxHours = 12;

    /**
     * Enable workload-based case assignment
     */
    private Boolean workloadBasedAssignment = true;

    /**
     * Maximum search days for next available slot
     */
    private Integer maxSearchDaysForSlot = 30;
}