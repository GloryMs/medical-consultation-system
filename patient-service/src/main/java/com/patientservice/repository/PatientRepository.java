package com.patientservice.repository;

import com.patientservice.entity.Patient;
import com.commonlibrary.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByUserId(Long userId);


    Optional<Patient> findByEmail(String email);

    boolean existsByUserId(Long userId);
    Long countBySubscriptionStatus(SubscriptionStatus status);
//    Long countByCasesStatus(CaseStatus status);
//
//    @Query("SELECT COUNT(cs) FROM case cs")
//    Long getAllCasesCount();
}