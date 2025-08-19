package com.patientservice.repository;

import com.patientservice.entity.Case;
import com.patientservice.entity.CaseStatus;
import com.patientservice.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseRepository extends JpaRepository<Case, Long> {
    @Query("SELECT C FROM Case C")
    List<Case> findAllCases();
    List<Case> findByPatientId(Long patientId);
    //List<Case> findByPatientIdAndStatus(Long patientId, CaseStatus status);
    //List<Case> findByAssignedDoctorId(Long doctorId);

    Long countByStatus(CaseStatus status);
    Long countByStatusIn(List<CaseStatus> statuses);

//    @Query("SELECT AVG(AGE(c.closedAt, c.createdAt)) FROM Case c WHERE c.status = 'CLOSED'")
//    String calculateAverageResolutionTime();

    //findByAssignedDoctorId
}