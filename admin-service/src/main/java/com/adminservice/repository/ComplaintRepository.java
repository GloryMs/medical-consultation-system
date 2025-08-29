package com.adminservice.repository;

import com.adminservice.entity.Complaint;
import com.commonlibrary.entity.ComplaintStatus;
import com.commonlibrary.entity.ComplaintPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByStatus(ComplaintStatus status);
    List<Complaint> findByPriority(ComplaintPriority priority);
    List<Complaint> findByStatusAndPriority(ComplaintStatus status, ComplaintPriority priority);
    List<Complaint> findByPatientId(Long patientId);
    List<Complaint> findByDoctorId(Long doctorId);
}