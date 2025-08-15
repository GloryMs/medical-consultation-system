package com.patientservice.repository;

import com.patientservice.entity.Patient;
import com.patientservice.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    Long countBySubscriptionStatus(SubscriptionStatus status);
}