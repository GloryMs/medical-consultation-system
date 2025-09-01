package com.patientservice.repository;

import com.patientservice.entity.CaseAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.commonlibrary.entity.*;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CaseAssignmentRepository extends JpaRepository<CaseAssignment, Long> {
    List<CaseAssignment> findByCaseEntityId(Long caseId);
    List<CaseAssignment> findByDoctorId(Long doctorId);
    List<CaseAssignment> findByDoctorIdAndStatus(Long doctorId, AssignmentStatus status);
    boolean existsByCaseEntityIdAndDoctorId(Long caseId, Long doctorId);
    List<CaseAssignment> findByStatusAndExpiresAtBefore(AssignmentStatus status, LocalDateTime dateTime);
    
    @Query("SELECT COUNT(ca) FROM CaseAssignment ca WHERE " +
            "ca.doctorId = :doctorId AND ca.status IN :statuses")
    long countActiveCasesByDoctor(@Param("doctorId") Long doctorId,
                                  @Param("statuses") List<AssignmentStatus> statuses);

    @Query("SELECT CA FROM CaseAssignment CA WHERE " +
            "CA.doctorId = :doctorId AND CA.status = :status")
    List<CaseAssignment> findByDoctorIdAndStatus(Long doctorId, String status);

    long countByStatus(AssignmentStatus status);

}