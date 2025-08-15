package com.patientservice.repository;

import com.patientservice.entity.Case;
import com.patientservice.entity.CaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseRepository extends JpaRepository<Case, Long> {
    List<Case> findByPatientId(Long patientId);
    List<Case> findByPatientIdAndStatus(Long patientId, CaseStatus status);
    List<Case> findByAssignedDoctorId(Long doctorId);

    Long countByStatus(CaseStatus status);
    Long countByStatusIn(List<CaseStatus> statuses);

    @Query("SELECT AVG(TIMESTAMPDIFF(HOUR, c.createdAt, c.closedAt)) FROM Case c WHERE c.status = 'CLOSED'")
    Double calculateAverageResolutionTime();
}