package com.patientservice.repository;

import com.patientservice.entity.CaseAssignment;
import com.patientservice.entity.CaseAssignmentReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CaseAssignmentReminderRepository extends JpaRepository<CaseAssignmentReminder, Long> {

    /**
     * Check if a reminder was already sent for a specific assignment and hour
     */
    boolean existsByAssignmentAndReminderHour(CaseAssignment assignment, Integer reminderHour);

    /**
     * Find all reminders for a specific assignment
     */
    List<CaseAssignmentReminder> findByAssignment(CaseAssignment assignment);

    /**
     * Find reminders sent after a specific time
     */
    @Query("SELECT COUNT(r) FROM CaseAssignmentReminder r WHERE r.sentAt > :cutoffTime")
    long countBysentAtAfter(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Delete old reminder records (for cleanup)
     */
    @Modifying
    @Query("DELETE FROM CaseAssignmentReminder r WHERE r.sentAt < :cutoffDate")
    int deleteBysentAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find reminders by assignment ID
     */
    @Query("SELECT r FROM CaseAssignmentReminder r WHERE r.assignment.id = :assignmentId ORDER BY r.sentAt DESC")
    List<CaseAssignmentReminder> findByAssignmentId(@Param("assignmentId") Long assignmentId);
}