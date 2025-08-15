package com.doctorservice.repository;

import com.doctorservice.entity.CalendarAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CalendarAvailabilityRepository extends JpaRepository<CalendarAvailability, Long> {
    List<CalendarAvailability> findByDoctorId(Long doctorId);
    List<CalendarAvailability> findByDoctorIdAndAvailableDate(Long doctorId, LocalDate date);
}
