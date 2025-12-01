package com.patientservice.repository;

import com.commonlibrary.entity.CaseStatus;
import com.patientservice.entity.Case;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseRepository extends JpaRepository<Case, Long> {
    @Query("SELECT C FROM Case C")
    List<Case> findAllCases();
    Optional<Case> findByIdAndIsDeletedFalse(Long id);
    List<Case> findAllCasesByIsDeletedFalse();
    List<Case> findByPatientId(Long patientId);
    List<Case> findByPatientIdAndIsDeletedFalse(Long patientId);

    List<Case> findByPatientIdAndStatus(Long patientId, CaseStatus status);
    //List<Case> findByAssignedDoctorId(Long doctorId);
    List<Case> findCaseByRequiredSpecializationAndStatusAndIsDeletedFalse(String specialization, CaseStatus caseStatus);

    Long countByStatusAndIsDeletedFalse(CaseStatus status);
    Long countByPatientIdAndStatusAndIsDeletedFalse(Long patientId, CaseStatus status);
    Long countByStatusInAndPatientIdAndIsDeletedFalse(List<CaseStatus> statuses, Long patientId);
    Long countByStatusInAndIsDeletedFalse(List<CaseStatus> statuses);
    Long countByPatientIdAndIsDeletedFalse(Long patientId);

//    @Query("SELECT AVG(AGE(c.closedAt, c.createdAt)) FROM Case c WHERE c.status = 'CLOSED'")
//    String calculateAverageResolutionTime();

    @Query("SELECT c FROM Case c WHERE c.patient.id = :patientId AND c.isDeleted != true ORDER BY c.submittedAt DESC LIMIT :limit ")
    List<Case> findLastSubmittedCases(@Param("patientId") Long patientId, @Param("limit") int limit);
//    List<Case> findTopByOrderBySubmittedAtDesc(int limit);

    // NEW: Dependent-related methods

    /**
     * Count cases for a specific dependent
     */
    Long countByDependentId(Long dependentId);

    /**
     * Count active cases for a dependent (excluding closed cases)
     */
    @Query("SELECT COUNT(c) FROM Case c WHERE c.dependent.id = :dependentId AND c.status NOT IN :excludedStatuses")
    Long countByDependentIdAndStatusNotIn(@Param("dependentId") Long dependentId,
                                          @Param("excludedStatuses") List<CaseStatus> excludedStatuses);

    /**
     * Find all cases for a dependent
     */
    Page<Case> findByDependentId(Long dependentId, Pageable pageable);

    /**
     * Find cases by patient (including their own and their dependents' cases)
     */
    @Query("SELECT c FROM Case c WHERE c.patient.id = :patientId ORDER BY c.createdAt DESC")
    Page<Case> findAllCasesByPatient(@Param("patientId") Long patientId, Pageable pageable);

    /**
     * Find case by ID and ensure it belongs to the patient (either directly or via dependent)
     */
    @Query("SELECT c FROM Case c WHERE c.id = :caseId AND c.patient.id = :patientId")
    Optional<Case> findByIdAndPatientIdIncludingDependents(@Param("caseId") Long caseId,
                                                           @Param("patientId") Long patientId);

    /**
     * Count cases that have reached or exceeded the maximum number of expirations
     * Used to identify cases requiring manual intervention
     */
    @Query("SELECT COUNT(DISTINCT c.id) FROM Case c " +
            "JOIN CaseAssignment ca ON ca.caseEntity.id = c.id " +
            "WHERE ca.status = 'EXPIRED' " +
            "GROUP BY c.id " +
            "HAVING COUNT(ca.id) >= :maxAttempts")
    long countCasesWithMultipleExpirations(@Param("maxAttempts") Integer maxAttempts);

    /**
     * Find cases with multiple expirations (for admin dashboard)
     */
    @Query("SELECT c FROM Case c " +
            "JOIN CaseAssignment ca ON ca.caseEntity.id = c.id " +
            "WHERE ca.status = 'EXPIRED' " +
            "GROUP BY c.id " +
            "HAVING COUNT(ca.id) >= :minExpirations " +
            "ORDER BY COUNT(ca.id) DESC")
    List<Case> findCasesWithMultipleExpirations(@Param("minExpirations") Integer minExpirations);
}