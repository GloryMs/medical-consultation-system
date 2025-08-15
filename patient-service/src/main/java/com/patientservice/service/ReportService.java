package com.patientservice.service;

import com.commonlibrary.exception.BusinessException;
import com.patientservice.dto.PatientReportDto;
import com.patientservice.entity.Case;
import com.patientservice.entity.Patient;
import com.patientservice.repository.CaseRepository;
import com.patientservice.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final PatientRepository patientRepository;
    private final CaseRepository caseRepository;

    public PatientReportDto generateMedicalHistoryReport(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        List<Case> cases = caseRepository.findByPatientId(patient.getId());

        PatientReportDto report = new PatientReportDto();
        report.setPatientName(patient.getFullName());
        report.setDateOfBirth(patient.getDateOfBirth());
        report.setBloodGroup(patient.getBloodGroup());
        report.setAllergies(patient.getAllergies());
        report.setChronicConditions(patient.getChronicConditions());
        report.setMedicalHistory(patient.getMedicalHistory());
        report.setTotalCases(cases.size());
        report.setCases(cases);
        report.setGeneratedAt(LocalDateTime.now());

        return report;
    }
}
