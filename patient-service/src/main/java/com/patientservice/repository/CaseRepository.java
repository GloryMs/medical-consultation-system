package com.patientservice.repository;

import com.commonlibrary.entity.CaseStatus;
import com.patientservice.entity.Case;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseRepository extends JpaRepository<Case, Long> {
    @Query("SELECT C FROM Case C")
    List<Case> findAllCases();
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

    //findByAssignedDoctorId
}