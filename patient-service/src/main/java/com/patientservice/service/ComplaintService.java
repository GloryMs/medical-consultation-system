package com.patientservice.service;

import com.commonlibrary.exception.BusinessException;
import com.patientservice.dto.ComplaintDto;
import com.patientservice.entity.Complaint;
import com.patientservice.entity.ComplaintStatus;
import com.patientservice.entity.Patient;
import com.patientservice.repository.ComplaintRepository;
import com.patientservice.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final PatientRepository patientRepository;

    @Transactional
    public Complaint submitComplaint(Long userId, ComplaintDto dto) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        Complaint complaint = Complaint.builder()
                .patientId(patient.getId())
                .doctorId(dto.getDoctorId())
                .caseId(dto.getCaseId())
                .complaintType(dto.getComplaintType())
                .description(dto.getDescription())
                .status(ComplaintStatus.OPEN)
                .priority(dto.getPriority())
                .build();

        return complaintRepository.save(complaint);
    }

    public List<Complaint> getPatientComplaints(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        return complaintRepository.findByPatientId(patient.getId());
    }
}
