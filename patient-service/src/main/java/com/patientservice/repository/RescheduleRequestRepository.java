package com.patientservice.repository;

import com.patientservice.entity.RescheduleRequest;
import com.patientservice.entity.RescheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RescheduleRequestRepository extends JpaRepository<RescheduleRequest, Long> {
    List<RescheduleRequest> findByCaseId(Long caseId);
    List<RescheduleRequest> findByStatus(RescheduleStatus status);
}