package com.doctorservice.repository;

import com.doctorservice.entity.ConsultationReport;
import com.doctorservice.entity.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultationReportRepository extends JpaRepository<ConsultationReport, Long> {
    List<ConsultationReport> findByDoctorId(Long doctorId);
    List<ConsultationReport> findByCaseId(Long caseId);

    // NEW METHODS:
    List<ConsultationReport> findByDoctorIdAndStatus(Long doctorId, ReportStatus status);
    Optional<ConsultationReport> findByCaseIdAndStatus(Long caseId, ReportStatus status);
    List<ConsultationReport> findByDoctorIdOrderByCreatedAtDesc(Long doctorId);
}
