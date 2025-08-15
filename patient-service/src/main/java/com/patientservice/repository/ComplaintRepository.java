package com.patientservice.repository;

import com.patientservice.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByPatientId(Long patientId);
    List<Complaint> findByDoctorId(Long doctorId);
    List<Complaint> findByCaseId(Long caseId);
}