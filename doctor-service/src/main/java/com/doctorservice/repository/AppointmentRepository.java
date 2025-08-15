package com.doctorservice.repository;

import com.doctorservice.entity.Appointment;
import com.doctorservice.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByDoctorId(Long doctorId);
    List<Appointment> findByDoctorIdAndStatus(Long doctorId, AppointmentStatus status);
    List<Appointment> findByDoctorIdAndScheduledTimeBetween(Long doctorId, LocalDateTime start, LocalDateTime end);
    List<Appointment> findByCaseId(Long caseId);

    Long countByDoctorIdAndScheduledTimeBetween(Long doctorId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.id = :doctorId AND a.status = :status AND a.scheduledTime BETWEEN :start AND :end")
    Long countByDoctorIdAndStatusAndScheduledTimeBetween(@Param("doctorId") Long doctorId,
                                                         @Param("status") String status,
                                                         @Param("start") LocalDateTime start,
                                                         @Param("end") LocalDateTime end);
}
