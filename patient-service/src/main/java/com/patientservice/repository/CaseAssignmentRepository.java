package com.patientservice.repository;

import com.patientservice.entity.Case;
import com.patientservice.entity.CaseAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.commonlibrary.entity.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CaseAssignmentRepository extends JpaRepository<CaseAssignment, Long> {
    List<CaseAssignment> findByCaseEntityId(Long caseId);
    List<CaseAssignment> findByDoctorId(Long doctorId);
    List<CaseAssignment> findByDoctorIdAndStatus(Long doctorId, AssignmentStatus status);
    boolean existsByCaseEntityIdAndDoctorId(Long caseId, Long doctorId);
    List<CaseAssignment> findByStatusAndExpiresAtBefore(AssignmentStatus status, LocalDateTime dateTim);
    List<CaseAssignment> findByCaseEntityIdIn(List<Long> caseIds);

    List<CaseAssignment> findByCaseEntityIn(List<Case> cases);

    Optional<CaseAssignment> findByCaseEntityAndDoctorId(Case caseEntity, Long doctorId);

    @Query("SELECT COUNT(ca) FROM CaseAssignment ca WHERE " +
            "ca.doctorId = :doctorId AND ca.status IN :statuses")
    long countActiveCasesByDoctor(@Param("doctorId") Long doctorId,
                                  @Param("statuses") List<AssignmentStatus> statuses);

//    @Query("SELECT CA FROM CaseAssignment CA WHERE " +
//            "CA.doctorId = :doctorId AND CA.status = :status")
//    List<CaseAssignment> findByDoctorIdAndStatus(Long doctorId, String status);

    long countByStatus(AssignmentStatus status);

    // ========== NEW METHODS FOR SCHEDULER ==========

    /**
     * Find assignments by case ID and status
     */
    List<CaseAssignment> findByCaseEntityIdAndStatus(Long caseId, AssignmentStatus status);

    /**
     * Count assignments by case ID and status
     */
    long countByCaseEntityIdAndStatus(Long caseId, AssignmentStatus status);

    /**
     * Find assignments by case, doctor, status, and responded after a time
     * Used for checking cooldown periods
     */
    @Query("SELECT ca FROM CaseAssignment ca WHERE ca.caseEntity.id = :caseId " +
            "AND ca.doctorId = :doctorId AND ca.status = :status " +
            "AND ca.respondedAt > :cutoffTime")
    List<CaseAssignment> findByCaseEntityIdAndDoctorIdAndStatusAndRespondedAtAfter(
            @Param("caseId") Long caseId,
            @Param("doctorId") Long doctorId,
            @Param("status") AssignmentStatus status,
            @Param("cutoffTime") LocalDateTime cutoffTime
    );

    /**
     * Find PENDING assignments in a time window (for reminders)
     */
    @Query("SELECT ca FROM CaseAssignment ca WHERE ca.status = :status " +
            "AND ca.assignedAt BETWEEN :windowStart AND :windowEnd")
    List<CaseAssignment> findByStatusAndAssignedAtBetween(
            @Param("status") AssignmentStatus status,
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd
    );

    /**
     * Count assignments by status and responded after a time
     */
    @Query("SELECT COUNT(ca) FROM CaseAssignment ca WHERE ca.status = :status " +
            "AND ca.respondedAt > :cutoffTime")
    long countByStatusAndRespondedAtAfter(
            @Param("status") AssignmentStatus status,
            @Param("cutoffTime") LocalDateTime cutoffTime
    );

    /**
     * Find all expired assignments for a specific case
     */
    @Query("SELECT ca FROM CaseAssignment ca WHERE ca.caseEntity.id = :caseId " +
            "AND ca.status = 'EXPIRED' ORDER BY ca.assignedAt DESC")
    List<CaseAssignment> findExpiredAssignmentsByCaseId(@Param("caseId") Long caseId);

    /**
     * Get distinct doctor IDs who have expired assignments for a case
     */
    @Query("SELECT DISTINCT ca.doctorId FROM CaseAssignment ca " +
            "WHERE ca.caseEntity.id = :caseId AND ca.status = 'EXPIRED'")
    List<Long> findExpiredDoctorIdsByCaseId(@Param("caseId") Long caseId);

}