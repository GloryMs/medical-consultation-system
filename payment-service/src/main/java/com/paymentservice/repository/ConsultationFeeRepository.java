package com.paymentservice.repository;

import com.paymentservice.entity.ConsultationFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultationFeeRepository extends JpaRepository<ConsultationFee, Long> {

    Optional<ConsultationFee> findBySpecializationAndIsActive(String specialization, boolean isActive);

    List<ConsultationFee> findByIsActive(boolean isActive);

    @Query("SELECT DISTINCT cf.specialization FROM ConsultationFee cf WHERE cf.isActive = true")
    List<String> findAllActiveSpecializations();

    boolean existsBySpecialization(String specialization);

    @Query("SELECT cf FROM ConsultationFee cf WHERE cf.isActive = true ORDER BY cf.specialization")
    List<ConsultationFee> findAllActiveFeesOrdered();
}