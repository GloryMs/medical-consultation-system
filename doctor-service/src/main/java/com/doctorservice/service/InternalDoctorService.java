package com.doctorservice.service;

import com.commonlibrary.dto.DoctorDto;
import com.doctorservice.dto.DoctorDetailsDto;
import com.commonlibrary.dto.PendingVerificationDto;
import com.doctorservice.entity.Doctor;
import com.commonlibrary.entity.VerificationStatus;
import com.doctorservice.repository.DoctorRepository;
import com.doctorservice.repository.AppointmentRepository;
import com.doctorservice.repository.ConsultationReportRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InternalDoctorService {
    
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final ConsultationReportRepository consultationReportRepository;
    
    public List<PendingVerificationDto> getPendingVerifications() {
        return doctorRepository.findByVerificationStatus(VerificationStatus.PENDING).stream()
                .map(this::mapToPendingVerificationDto)
                .collect(Collectors.toList());
    }
    
    public Long getPendingVerificationsCount() {
        return doctorRepository.countByVerificationStatus(VerificationStatus.PENDING);
    }
    
    public DoctorDetailsDto getDoctorDetails(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        
        DoctorDetailsDto dto = new DoctorDetailsDto();
        dto.setId(doctor.getId());
        dto.setFullName(doctor.getFullName());
        dto.setLicenseNumber(doctor.getLicenseNumber());
        dto.setPrimarySpecialization(doctor.getPrimarySpecialization());
        dto.setSubSpecialization("Sub Specialization .. Added by me");
        dto.setAverageRating(doctor.getRating());
        dto.setConsultationCount(doctor.getConsultationCount());
        
        return dto;
    }
    
    public Map<String, Object> getDoctorPerformance(Long doctorId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> performance = new HashMap<>();
        
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        
        // Get appointment statistics
        Long totalAppointments = appointmentRepository.countByDoctorIdAndScheduledTimeBetween(doctorId, start, end);
        Long completedAppointments = appointmentRepository.countByDoctorIdAndStatusAndScheduledTimeBetween(
                doctorId, "COMPLETED", start, end);
        Long cancelledAppointments = appointmentRepository.countByDoctorIdAndStatusAndScheduledTimeBetween(
                doctorId, "CANCELLED", start, end);
        
        performance.put("totalConsultations", totalAppointments.intValue());
        performance.put("completedConsultations", completedAppointments.intValue());
        performance.put("cancelledAppointments", cancelledAppointments.intValue());
        
        // Get doctor rating
        Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
        if (doctor != null) {
            performance.put("averageRating", doctor.getRating());
        }
        
        // Add other metrics
        performance.put("totalRevenue", calculateDoctorRevenue(doctorId, start, end));
        performance.put("averageConsultationTime", 45); // minutes
        performance.put("satisfactionScore", 4.5);
        performance.put("averageResponseTime", 2.5); // hours
        
        return performance;
    }
    
    private PendingVerificationDto mapToPendingVerificationDto(Doctor doctor) {
        PendingVerificationDto dto = new PendingVerificationDto();
        dto.setDoctorId(doctor.getId());
        dto.setFullName(doctor.getFullName());
        dto.setLicenseNumber(doctor.getLicenseNumber());
        dto.setSpecialization("Sub Specialization .. Added by me");
        dto.setSubmittedAt(doctor.getCreatedAt());
        return dto;
    }
    
    private Double calculateDoctorRevenue(Long doctorId, LocalDateTime start, LocalDateTime end) {
        // In real implementation, this would query payment service
        return 5000.0; // Placeholder
    }

    public List<DoctorDto> getDoctorsBySpecialization (String specialization, int limit) {
        List<DoctorDto> doctors = new ArrayList<>();
        List<Doctor> doctorList = doctorRepository.findAvailableDoctorsBySpecialization( specialization, limit );
        if( doctorList != null && !doctorList.isEmpty() ) {
            doctors = doctorList.stream().map(this :: convertToDoctorDto).toList();
        }
        return doctors;
    }

    public DoctorDto convertToDoctorDto(Doctor doctor){
        DoctorDto dto = new DoctorDto();
        ModelMapper modelMapper = new ModelMapper();
        dto = modelMapper.map(doctor, DoctorDto.class);
        return dto;
    }

    public DoctorDetailsDto convertToDoctorDetailsDto(Doctor doctor) {

        DoctorDetailsDto dto = new DoctorDetailsDto();
        dto.setId(doctor.getId());
        dto.setUserId(doctor.getUserId());
        dto.setFullName(doctor.getFullName());
        dto.setLicenseNumber(doctor.getLicenseNumber());
        dto.setPrimarySpecialization(doctor.getPrimarySpecialization());
        dto.setSubSpecialization(doctor.getPrimarySpecialization());
        dto.setAverageRating(doctor.getRating());
        dto.setConsultationCount(doctor.getConsultationCount());

        return dto;
    }

}