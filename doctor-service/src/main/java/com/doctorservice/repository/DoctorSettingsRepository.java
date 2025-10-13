package com.doctorservice.repository;

import com.doctorservice.entity.DoctorSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorSettingsRepository extends JpaRepository<DoctorSettings, Long> {
    Optional<DoctorSettings> findByDoctorId(Long doctorId);
    boolean existsByDoctorId(Long doctorId);
}