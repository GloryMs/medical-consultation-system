package com.doctorservice.repository;

import com.doctorservice.entity.ConsultationReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsultationReportRepository extends JpaRepository<ConsultationReport, Long> {
    List<ConsultationReport> findByDoctorId(Long doctorId);
    List<ConsultationReport> findByCaseId(Long caseId);
}
