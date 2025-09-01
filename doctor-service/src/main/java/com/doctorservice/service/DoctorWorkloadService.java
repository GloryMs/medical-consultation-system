// DoctorWorkloadService.java
package com.doctorservice.service;

import com.commonlibrary.entity.AppointmentStatus;
import com.commonlibrary.entity.VerificationStatus;
import com.commonlibrary.exception.BusinessException;
import com.doctorservice.dto.DoctorWorkloadDto;
import com.doctorservice.dto.WorkloadMetricsDto;
import com.doctorservice.entity.Doctor;
import com.doctorservice.entity.Appointment;
import com.doctorservice.feign.PatientServiceClient;
import com.doctorservice.repository.DoctorRepository;
import com.doctorservice.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorWorkloadService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientServiceClient patientServiceClient;

    @Value("${doctor.workload.max-active-cases:10}")
    private Integer maxActiveCases;

    @Value("${doctor.workload.max-daily-appointments:8}")
    private Integer maxDailyAppointments;

    @Value("${doctor.workload.buffer-minutes:15}")
    private Integer bufferMinutes;

    /**
     * Calculate and update doctor's current workload
     * This method should be called whenever case assignments change
     */
    @Transactional
    public void loadDoctorWorkload(Long doctorId) {
        log.info("Loading workload for doctor: {}", doctorId);

        try {
            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

            // Calculate current workload metrics
            WorkloadMetricsDto metrics = calculateWorkloadMetrics(doctorId);

            // Update doctor availability based on workload
            updateDoctorAvailabilityStatus(doctor, metrics);

            // Update workload in doctor entity
            updateDoctorWorkloadFields(doctor, metrics);

            doctorRepository.save(doctor);

            log.info("Workload updated for doctor {}: Active cases: {}, Today's appointments: {}, Available: {}",
                    doctorId, metrics.getActiveCases(), metrics.getTodayAppointments(), doctor.getIsAvailable());

        } catch (Exception e) {
            log.error("Error loading workload for doctor {}: {}", doctorId, e.getMessage(), e);
            throw new BusinessException("Failed to load doctor workload", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Check if doctor is available at a specific time
     */
    public boolean isDoctorAvailable(Long doctorId, LocalDateTime requestedTime) {
        log.debug("Checking availability for doctor {} at {}", doctorId, requestedTime);

        try {
            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

            // Check if doctor is generally available
            if (!doctor.getIsAvailable()) {
                log.debug("Doctor {} is marked as unavailable", doctorId);
                return false;
            }

            // Check workload capacity
            if (!hasCapacityForNewCase(doctorId)) {
                log.debug("Doctor {} has reached maximum workload capacity", doctorId);
                return false;
            }

            // Check specific time slot availability
            if (!isTimeSlotAvailable(doctor, requestedTime)) {
                log.debug("Doctor {} is not available at requested time {}", doctorId, requestedTime);
                return false;
            }

            // Check for conflicting appointments
            if (hasConflictingAppointment(doctorId, requestedTime)) {
                log.debug("Doctor {} has conflicting appointment at {}", doctorId, requestedTime);
                return false;
            }

            log.debug("Doctor {} is available at {}", doctorId, requestedTime);
            return true;

        } catch (Exception e) {
            log.error("Error checking availability for doctor {}: {}", doctorId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get detailed workload information for a doctor
     */
    public DoctorWorkloadDto getDoctorWorkload(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        WorkloadMetricsDto metrics = calculateWorkloadMetrics(doctorId);

        return DoctorWorkloadDto.builder()
                .doctorId(doctorId)
                .isAvailable(doctor.getIsAvailable())
                .activeCases(metrics.getActiveCases())
                .maxActiveCases(maxActiveCases)
                .todayAppointments(metrics.getTodayAppointments())
                .maxDailyAppointments(maxDailyAppointments)
                .thisWeekAppointments(metrics.getThisWeekAppointments())
                .consultationCount(doctor.getConsultationCount())
                .averageRating(doctor.getRating())
                .workloadPercentage(calculateWorkloadPercentage(metrics))
                .nextAvailableSlot(findNextAvailableSlot(doctor))
                .upcomingAppointments(getUpcomingAppointments(doctorId, 5))
                .build();
    }

    /**
     * Calculate comprehensive workload metrics
     */
    private WorkloadMetricsDto calculateWorkloadMetrics(Long doctorId) {
        // Get active case assignments from patient service
        Integer activeCases = 0;
        try {
            var response = patientServiceClient.getDoctorActiveCases(doctorId);
            if (response != null && response.getBody() != null) {
                activeCases = response.getBody().getData().size();
            }
        } catch (Exception e) {
            log.warn("Failed to get active cases for doctor {}: {}", doctorId, e.getMessage());
        }

        // Calculate appointment metrics
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        LocalDateTime weekStart = todayStart.minusDays(todayStart.getDayOfWeek().getValue() - 1);
        LocalDateTime weekEnd = weekStart.plusDays(7);

        List<Appointment> todayAppointments = appointmentRepository
                .findByDoctorIdAndScheduledTimeBetween(doctorId, todayStart, todayEnd);

        List<Appointment> weekAppointments = appointmentRepository
                .findByDoctorIdAndScheduledTimeBetween(doctorId, weekStart, weekEnd);

        return WorkloadMetricsDto.builder()
                .activeCases(activeCases)
                .todayAppointments(todayAppointments.size())
                .thisWeekAppointments(weekAppointments.size())
                .build();
    }

    /**
     * Update doctor's availability status based on workload
     */
    private void updateDoctorAvailabilityStatus(Doctor doctor, WorkloadMetricsDto metrics) {
        boolean shouldBeAvailable = doctor.getIsAvailable();

        // Auto-disable if workload exceeds limits
        if (metrics.getActiveCases() >= maxActiveCases) {
            shouldBeAvailable = false;
            log.info("Auto-disabling doctor {} due to maximum active cases reached", doctor.getId());
        }

        if (metrics.getTodayAppointments() >= maxDailyAppointments) {
            shouldBeAvailable = false;
            log.info("Auto-disabling doctor {} due to maximum daily appointments reached", doctor.getId());
        }

        doctor.setIsAvailable(shouldBeAvailable);
    }

    /**
     * Update workload-related fields in doctor entity
     */
    private void updateDoctorWorkloadFields(Doctor doctor, WorkloadMetricsDto metrics) {
        doctor.setActiveCases(metrics.getActiveCases());
        doctor.setTodayAppointments(metrics.getTodayAppointments());
        doctor.setWorkloadPercentage(calculateWorkloadPercentage(metrics));
        doctor.setLastWorkloadUpdate(LocalDateTime.now());
    }

    /**
     * Check if doctor has capacity for new cases
     */
    private boolean hasCapacityForNewCase(Long doctorId) {
        WorkloadMetricsDto metrics = calculateWorkloadMetrics(doctorId);
        return metrics.getActiveCases() < maxActiveCases &&
               metrics.getTodayAppointments() < maxDailyAppointments;
    }

    /**
     * Check if specific time slot is available based on doctor's schedule
     */
    private boolean isTimeSlotAvailable(Doctor doctor, LocalDateTime requestedTime) {
        if (doctor.getAvailableTimeSlots() == null || doctor.getAvailableTimeSlots().isEmpty()) {
            // If no specific time slots defined, assume 24/7 availability
            return true;
        }

        DayOfWeek requestedDay = requestedTime.getDayOfWeek();
        LocalTime requestedLocalTime = requestedTime.toLocalTime();

        return doctor.getAvailableTimeSlots().stream()
                .anyMatch(slot -> slot.getDayOfWeek().equals(requestedDay) &&
                        slot.getIsAvailable() &&
                        !requestedLocalTime.isBefore(slot.getStartTime()) &&
                        !requestedLocalTime.isAfter(slot.getEndTime()));
    }

    /**
     * Check for conflicting appointments
     */
    private boolean hasConflictingAppointment(Long doctorId, LocalDateTime requestedTime) {
        LocalDateTime bufferStart = requestedTime.minusMinutes(bufferMinutes);
        LocalDateTime bufferEnd = requestedTime.plusMinutes(bufferMinutes);

        List<Appointment> conflictingAppointments = appointmentRepository
                .findByDoctorIdAndScheduledTimeBetweenAndStatusIn(
                        doctorId,
                        bufferStart,
                        bufferEnd,
                        Arrays.asList(AppointmentStatus.SCHEDULED, AppointmentStatus.RESCHEDULED,
                                AppointmentStatus.CONFIRMED)
                );

        return !conflictingAppointments.isEmpty();
    }

    /**
     * Calculate workload percentage
     */
    private Double calculateWorkloadPercentage(WorkloadMetricsDto metrics) {
        double caseLoad = (double) metrics.getActiveCases() / maxActiveCases * 50; // 50% weight for cases
        double appointmentLoad = (double) metrics.getTodayAppointments() / maxDailyAppointments * 50; // 50% weight for appointments
        return Math.min(100.0, caseLoad + appointmentLoad);
    }

    /**
     * Find next available appointment slot
     */
    private LocalDateTime findNextAvailableSlot(Doctor doctor) {
        LocalDateTime searchStart = LocalDateTime.now();
        LocalDateTime searchEnd = searchStart.plusDays(30); // Search next 30 days

        // Start with next hour
        LocalDateTime candidate = searchStart.plusHours(1).withMinute(0).withSecond(0).withNano(0);

        while (candidate.isBefore(searchEnd)) {
            if (isDoctorAvailable(doctor.getId(), candidate)) {
                return candidate;
            }
            candidate = candidate.plusHours(1); // Check hourly slots
        }

        return null; // No available slot found in next 30 days
    }

    /**
     * Get upcoming appointments for the doctor
     */
    private List<Appointment> getUpcomingAppointments(Long doctorId, int limit) {
        LocalDateTime now = LocalDateTime.now();
        return appointmentRepository
                .findByDoctorIdAndScheduledTimeAfterOrderByScheduledTimeAsc(doctorId, now)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Recalculate availability for all doctors (batch operation)
     */
    @Transactional
    public void recalculateAllDoctorWorkloads() {
        log.info("Starting batch workload recalculation for all doctors");

        List<Doctor> allDoctors = doctorRepository.findAll();
        int updated = 0;

        for (Doctor doctor : allDoctors) {
            try {
                loadDoctorWorkload(doctor.getId());
                updated++;
            } catch (Exception e) {
                log.error("Failed to update workload for doctor {}: {}", doctor.getId(), e.getMessage());
            }
        }

        log.info("Batch workload recalculation completed. Updated {} out of {} doctors", updated, allDoctors.size());
    }

    /**
     * Get doctors with low workload for case assignment
     */
    public List<Long> getAvailableDoctorsForAssignment(String specialization, int limit) {
        List<Doctor> availableDoctors = doctorRepository
                .findbyVerificationStatusAndIsAvailable(VerificationStatus.VERIFIED, true);

        return availableDoctors.stream()
                .filter(doctor -> specialization == null || 
                        specialization.equals(doctor.getPrimarySpecialization()))
                .filter(doctor -> hasCapacityForNewCase(doctor.getId()))
                .sorted(Comparator.comparing(Doctor::getWorkloadPercentage))
                .limit(limit)
                .map(Doctor::getId)
                .collect(Collectors.toList());
    }

    /**
     * Emergency workload management - temporarily increase capacity
     */
    @Transactional
    public void enableEmergencyMode(Long doctorId, String reason) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        doctor.setIsAvailable(true);
        doctor.setEmergencyMode(true);
        doctor.setEmergencyModeReason(reason);
        doctor.setEmergencyModeEnabledAt(LocalDateTime.now());

        doctorRepository.save(doctor);

        log.info("Emergency mode enabled for doctor {}: {}", doctorId, reason);
    }

    /**
     * Disable emergency mode
     */
    @Transactional
    public void disableEmergencyMode(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        doctor.setEmergencyMode(false);
        doctor.setEmergencyModeReason(null);
        doctor.setEmergencyModeEnabledAt(null);

        // Recalculate normal availability
        loadDoctorWorkload(doctorId);

        doctorRepository.save(doctor);

        log.info("Emergency mode disabled for doctor {}", doctorId);
    }
}