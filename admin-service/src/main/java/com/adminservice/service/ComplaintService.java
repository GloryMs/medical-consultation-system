package com.adminservice.service;

import com.commonlibrary.dto.ComplaintDto;
import com.adminservice.dto.ComplaintResponseDto;
import com.adminservice.entity.Complaint;
import com.adminservice.feign.NotificationServiceClient;
import com.adminservice.repository.ComplaintRepository;
import com.commonlibrary.entity.ComplaintPriority;
import com.commonlibrary.entity.ComplaintStatus;
import com.commonlibrary.entity.ComplaintType;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final NotificationServiceClient notificationServiceClient;
    //private final PatientRepository patientRepository;

    @Transactional
    public Complaint submitComplaint(ComplaintDto dto) {
        Complaint complaint = Complaint.builder()
                .patientId(dto.getPatientId())
                .doctorId(dto.getDoctorId())
                .caseId(dto.getCaseId())
                .complaintType(ComplaintType.valueOf(dto.getComplaintType()))
                .description(dto.getDescription())
                .status(ComplaintStatus.OPEN)
                .priority( ComplaintPriority.valueOf(dto.getPriority()))
                .build();

        return complaintRepository.save(complaint);
    }

    public List<ComplaintDto> getPatientComplaints(Long userId) {
        return complaintRepository.findByPatientId(userId).stream().map(this::convertToComplaintDto).collect(Collectors.toList());
    }

    // 36. Get All Complaints Implementation
    public List<ComplaintDto> getAllComplaints(String status, String priority) {
        if (status != null && priority != null) {
            return complaintRepository.findByStatusAndPriority(
                    ComplaintStatus.valueOf(status),
                    ComplaintPriority.valueOf(priority)).stream().
                    map(this::convertToComplaintDto).collect(Collectors.toList());
        } else if (status != null) {
            return complaintRepository.findByStatus(ComplaintStatus.valueOf(status)).
                    stream().map(this::convertToComplaintDto).collect(Collectors.toList());
        } else if (priority != null) {
            return complaintRepository.findByPriority(ComplaintPriority.valueOf(priority)).
                    stream().map(this::convertToComplaintDto).collect(Collectors.toList());
        } else {
            return complaintRepository.findAll().stream().map(this::convertToComplaintDto).collect(Collectors.toList());
        }
    }

    public ComplaintDto convertToComplaintDto(Complaint complaint) {
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(complaint, ComplaintDto.class);
    }

    @Transactional
    public void respondToComplaint(Long complaintId, ComplaintResponseDto dto) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        complaint.setAdminResponse(dto.getResponse());
        complaint.setStatus(ComplaintStatus.valueOf(dto.getStatus()));
        complaint.setResolvedAt(LocalDateTime.now());

        complaintRepository.save(complaint);

        //TODO handle this:
        // Notify the patient
//        notificationServiceClient.sendNotification(
//                0L, // System notification
//                complaint.getPatientId(),
//                "Complaint Response",
//                "Your complaint has been reviewed. Status: " + dto.getStatus()
//        );
    }
}
