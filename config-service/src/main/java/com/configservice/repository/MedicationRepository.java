package com.configservice.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.configservice.entity.*;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, Long> {
    List<Medication> findByIsActiveTrueOrderByName();
    List<Medication> findByCategoryAndIsActiveTrueOrderByName(String category);
    Optional<Medication> findByAtcCodeAndIsActiveTrue(String atcCode);
    List<Medication> findByIndicationsContaining(String indication);
}