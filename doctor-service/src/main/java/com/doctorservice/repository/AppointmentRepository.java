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

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByDoctorId(Long doctorId);
    List<Appointment> findByDoctorIdAndStatus(Long doctorId, AppointmentStatus status);
    List<Appointment> findByDoctorIdAndScheduledTimeBetween(Long doctorId, LocalDateTime start, LocalDateTime end);
    List<Appointment> findByCaseId(Long caseId);
    List<Appointment> findByPatientId(Long patientId);

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
}
