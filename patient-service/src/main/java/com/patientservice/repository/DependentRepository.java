package com.patientservice.repository;

import com.patientservice.entity.Dependent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DependentRepository extends JpaRepository<Dependent, Long> {
    
    /**
     * Find all dependents for a specific patient
     */
    List<Dependent> findByPatientIdAndIsDeletedFalse(Long patientId);

    /**
     * Find a specific dependent by ID and patient ID (for security)
     */
    Optional<Dependent> findByIdAndPatientIdAndIsDeletedFalse(Long dependentId, Long patientId);

    /**
     * Count total dependents for a patient
     */
    Long countByPatientIdAndIsDeletedFalse(Long patientId);

    /**
     * Check if dependent exists by name and patient
     */
    boolean existsByFullNameAndPatientIdAndIsDeletedFalse(String fullName, Long patientId);
}