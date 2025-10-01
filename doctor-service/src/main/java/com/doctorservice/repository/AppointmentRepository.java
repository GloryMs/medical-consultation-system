package com.doctorservice.repository;

import com.commonlibrary.entity.AssignmentStatus;
import com.doctorservice.entity.Appointment;
import com.commonlibrary.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByDoctorId(Long doctorId);
    List<Appointment> findByDoctorIdAndStatus(Long doctorId, AppointmentStatus status);
    List<Appointment> findByDoctorIdAndScheduledTimeBetween(Long doctorId, LocalDateTime start, LocalDateTime end);
    List<Appointment> findByCaseId(Long caseId);
    List<Appointment> findByPatientId(Long patientId);
    Optional<Appointment> findByCaseIdAndPatientIdAndDoctorId(Long caseId, Long patientId, Long doctorId);

    Long countByDoctorIdAndScheduledTimeBetween(Long doctorId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.id = :doctorId AND a.status = :status AND a.scheduledTime BETWEEN :start AND :end")
    Long countByDoctorIdAndStatusAndScheduledTimeBetween(@Param("doctorId") Long doctorId,
                                                         @Param("status") String status,
                                                         @Param("start") LocalDateTime start,
                                                         @Param("end") LocalDateTime end);

    List<Appointment> findByDoctorIdAndScheduledTimeBetweenAndStatusIn(
            Long doctorId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            List<AppointmentStatus> statuses
    );

    List<Appointment> findByDoctorIdAndScheduledTimeAfterOrderByScheduledTimeAsc(
            Long doctorId,
            LocalDateTime scheduledTime
    );


    @Query("SELECT COUNT(a) FROM Appointment a WHERE " +
            "a.doctor.id = :doctorId AND " +
            "a.scheduledTime BETWEEN :startTime AND :endTime AND " +
            "a.status IN :statuses")
    long countAppointmentsByDoctorAndTimeRangeAndStatus(
            @Param("doctorId") Long doctorId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("statuses") List<AppointmentStatus> statuses
    );

    @Query("SELECT a FROM Appointment a WHERE " +
            "a.doctor.id = :doctorId AND " +
            "a.scheduledTime >= :fromTime AND " +
            "a.status = :status " +
            "ORDER BY a.scheduledTime ASC")
    List<Appointment> findUpcomingAppointments(
            @Param("doctorId") Long doctorId,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("status") AppointmentStatus status
    );


    @Query("SELECT a FROM Appointment a WHERE " +
            "a.scheduledTime BETWEEN :startTime AND :endTime AND " +
            "a.status IN :statuses AND " + ////('SCHEDULED', 'RESCHEDULED, CONFIRMED')
            "a.doctor.id = :doctorId")
    List<Appointment> findConflictingAppointments(
            @Param("doctorId") Long doctorId,
            @Param("startTime") LocalDateTime startTime,
            @Param("statuses") List<AppointmentStatus> statuses,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("SELECT COUNT(a) FROM Appointment a WHERE " +
            "a.doctor.id = :doctorId AND " +
            "DATE(a.scheduledTime) = :currentDate AND " +
            "a.status NOT IN :statuses") //('CANCELLED', 'NO_SHOW')
    long countTodayAppointmentsByDoctor(@Param("doctorId") Long doctorId,
                                        @Param("currentDate") LocalDateTime currentDate,
                                        @Param("statuses") List<AppointmentStatus> statuses);

    @Query("SELECT AVG(a.duration) FROM Appointment a WHERE " +
            "a.doctor.id = :doctorId AND " +
            "a.status = 'COMPLETED' AND " +
            "a.scheduledTime >= :fromDate")
    Double getAverageAppointmentDuration(
            @Param("doctorId") Long doctorId,
            @Param("fromDate") LocalDateTime fromDate
    );

    //com.commonlibrary.entity.AppointmentStatus.


    /**
     * Find appointments for a doctor within a time range, excluding cancelled ones
     * Used for conflict detection
     */
    List<Appointment> findByDoctorIdAndScheduledTimeBetweenAndStatusNot(
            Long doctorId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            AppointmentStatus excludeStatus
    );

    /**
     * Alternative query using @Query annotation for more complex filtering
     */
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId " +
            "AND a.scheduledTime BETWEEN :startTime AND :endTime " +
            "AND a.status NOT IN :excludeStatuses")
    List<Appointment> findDoctorAppointmentsInTimeRange(
            @Param("doctorId") Long doctorId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeStatuses") List<AppointmentStatus> excludeStatuses
    );

    /**
     * Find all appointments for a doctor on a specific date
     */
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId " +
            "AND DATE(a.scheduledTime) = DATE(:date) " +
            "AND a.status != :excludeStatus " +
            "ORDER BY a.scheduledTime ASC")
    List<Appointment> findDoctorAppointmentsByDate(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDateTime date,
            @Param("excludeStatus") AppointmentStatus excludeStatus
    );

    /**
     * Check if doctor has any appointments during a specific time window
     */
    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.doctor.id = :doctorId " +
            "AND a.scheduledTime < :endTime " +
            "AND FUNCTION('DATE_ADD', a.scheduledTime, a.duration, 'MINUTE') > :startTime " +
            "AND a.status NOT IN :excludeStatuses " +
            "AND (:excludeAppointmentId IS NULL OR a.id != :excludeAppointmentId)")
    boolean hasConflictingAppointment(
            @Param("doctorId") Long doctorId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeStatuses") List<AppointmentStatus> excludeStatuses,
            @Param("excludeAppointmentId") Long excludeAppointmentId
    );

    /**
     * Find appointments that overlap with a given time range
     * Using native query for better database compatibility
     */
    @Query(value = "SELECT * FROM appointments a WHERE a.doctor_id = :doctorId " +
            "AND a.scheduled_time < :endTime " +
            "AND DATE_ADD(a.scheduled_time, INTERVAL a.duration MINUTE) > :startTime " +
            "AND a.status NOT IN ('CANCELLED', 'NO_SHOW') " +
            "AND (:excludeId IS NULL OR a.id != :excludeId)",
            nativeQuery = true)
    List<Appointment> findOverlappingAppointments(
            @Param("doctorId") Long doctorId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeId") Long excludeId
    );

    /**
     * Alternative JPQL approach - Find appointments that might overlap
     * Then filter in service layer for precise overlap detection
     */
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId " +
            "AND a.scheduledTime >= :searchStart " +
            "AND a.scheduledTime <= :searchEnd " +
            "AND a.status NOT IN (com.commonlibrary.entity.AppointmentStatus.CANCELLED," +
            " com.commonlibrary.entity.AppointmentStatus.NO_SHOW) " +
            "AND (:excludeId IS NULL OR a.id != :excludeId)")
    List<Appointment> findPotentialOverlappingAppointments(
            @Param("doctorId") Long doctorId,
            @Param("searchStart") LocalDateTime searchStart,
            @Param("searchEnd") LocalDateTime searchEnd,
            @Param("excludeId") Long excludeId
    );
}
