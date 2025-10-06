package com.doctorservice.repository;

import com.doctorservice.entity.AppointmentReminder;
import com.doctorservice.entity.ReminderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentReminderRepository extends JpaRepository<AppointmentReminder, Long> {

    /**
     * Find pending reminders that should be sent within the time window
     */
    @Query("SELECT r FROM AppointmentReminder r WHERE " +
           "r.status = 'PENDING' AND " +
           "r.scheduledSendTime <= :windowEnd AND " +
           "r.scheduledSendTime >= :now")
    List<AppointmentReminder> findPendingRemindersInTimeWindow(
            @Param("now") LocalDateTime now,
            @Param("windowEnd") LocalDateTime windowEnd
    );

    /**
     * Find all reminders for a specific appointment
     */
    List<AppointmentReminder> findByAppointmentId(Long appointmentId);

    /**
     * Find reminders by appointment and status
     */
    List<AppointmentReminder> findByAppointmentIdAndStatus(
            Long appointmentId, 
            ReminderStatus status
    );

    /**
     * Find all pending reminders for a specific recipient
     */
    @Query("SELECT r FROM AppointmentReminder r WHERE " +
           "r.recipientUserId = :userId AND " +
           "r.status = 'PENDING' " +
           "ORDER BY r.scheduledSendTime ASC")
    List<AppointmentReminder> findPendingRemindersByRecipient(@Param("userId") Long userId);

    /**
     * Find overdue reminders (should have been sent but weren't)
     */
    @Query("SELECT r FROM AppointmentReminder r WHERE " +
           "r.status = 'PENDING' AND " +
           "r.scheduledSendTime < :cutoffTime")
    List<AppointmentReminder> findOverdueReminders(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Count pending reminders for an appointment
     */
    @Query("SELECT COUNT(r) FROM AppointmentReminder r WHERE " +
           "r.appointment.id = :appointmentId AND " +
           "r.status = 'PENDING'")
    long countPendingRemindersByAppointment(@Param("appointmentId") Long appointmentId);

    /**
     * Delete old sent/failed reminders (cleanup)
     */
    @Query("DELETE FROM AppointmentReminder r WHERE " +
           "r.status IN (com.doctorservice.entity.ReminderStatus.SENT," +
            " com.doctorservice.entity.ReminderStatus.FAILED," +
            " com.doctorservice.entity.ReminderStatus.CANCELLED) AND " +
           "r.scheduledSendTime < :cutoffDate")
    void deleteOldReminders(@Param("cutoffDate") LocalDateTime cutoffDate);
}