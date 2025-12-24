package com.doctorservice.service;

import com.commonlibrary.dto.*;
import com.commonlibrary.exception.BusinessException;
import com.doctorservice.dto.DoctorDetailsDto;
import com.doctorservice.entity.Doctor;
import com.commonlibrary.entity.VerificationStatus;
import com.doctorservice.kafka.DoctorEventProducer;
import com.doctorservice.repository.DoctorRepository;
import com.doctorservice.repository.AppointmentRepository;
import com.doctorservice.repository.ConsultationReportRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InternalDoctorService {
    
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final ConsultationReportRepository consultationReportRepository;
    private final DoctorEventProducer doctorEventProducer;

    public List<PendingVerificationDto> getPendingVerifications() {
        List<PendingVerificationDto> pendingVerificationDtos = doctorRepository.findByVerificationStatus(VerificationStatus.PENDING).stream()
                .map(this::mapToPendingVerificationDto)
                .collect(Collectors.toList());
        log.info("Count of doctors' pending verifications: {}", pendingVerificationDtos.size());
        return pendingVerificationDtos;
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
        dto.setSpecialization(doctor.getPrimarySpecialization());
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

    /**
     * Get complete doctor verification details for admin review
     * This method retrieves all necessary information for admin to verify a doctor
     */
    public DoctorVerificationDetailsDto getDoctorVerificationDetails(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BusinessException("Doctor not found with ID: " + doctorId, HttpStatus.NOT_FOUND));

        // Build comprehensive verification details
        return DoctorVerificationDetailsDto.builder()
                // Basic Information
                .doctorId(doctor.getId())
                .userId(doctor.getUserId())
                .fullName(doctor.getFullName())
                .email(doctor.getEmail())
                .phoneNumber(doctor.getPhoneNumber())
                .licenseNumber(doctor.getLicenseNumber())

                // Specialization
                .primarySpecialization(doctor.getPrimarySpecialization())
                .subSpecializations(doctor.getSubSpecializations())

                // Professional Information
                .yearsOfExperience(doctor.getYearsOfExperience())
                .professionalSummary(doctor.getProfessionalSummary())
                .qualifications(doctor.getQualifications())
                .certifications(doctor.getCertifications() != null ? doctor.getCertifications() : Set.of())
                .languages(doctor.getLanguages())

                // Location
                .hospitalAffiliation(doctor.getHospitalAffiliation())
                .address(doctor.getAddress())
                .city(doctor.getCity())
                .country(doctor.getCountry())

                // Expertise
                .diseaseExpertiseCodes(doctor.getDiseaseExpertiseCodes())
                .symptomExpertiseCodes(doctor.getSymptomExpertiseCodes())

                // Pricing
                .hourlyRate(doctor.getHourlyRate())
                .caseRate(doctor.getCaseRate())
                .emergencyRate(doctor.getEmergencyRate())

                // Verification Status
                .verificationStatus(doctor.getVerificationStatus().name())
                .submittedAt(doctor.getCreatedAt())
                .verifiedAt(doctor.getVerifiedAt())

                // Documents - These should be fetched from file storage service
                // For now, we'll use placeholder URLs or construct them based on doctor ID
                .documentsUrl(constructDocumentsUrl(doctor.getId()))
                .medicalLicenseUrl(constructDocumentUrl(doctor.getId(), "medical_license"))
                .medicalDegreeUrl(constructDocumentUrl(doctor.getId(), "medical_degree"))
                .certificationsUrl(constructDocumentUrl(doctor.getId(), "certifications"))
                .identityDocumentUrl(constructDocumentUrl(doctor.getId(), "identity"))

                // Statistics
                .consultationCount(doctor.getConsultationCount())
                .rating(doctor.getRating())
                .totalRatings(doctor.getTotalRatings())

                // Additional verification info
                .verificationNotes(doctor.getVerificationNotes())
                .rejectionReason(doctor.getRejectionReason())
                .build();
    }

    /**
     * Helper method to construct document URLs
     * In production, this should integrate with your file storage service
     */
    private String constructDocumentsUrl(Long doctorId) {
        // This should return the actual document storage URL
        // Example: return fileStorageService.getDocumentUrl(doctorId, "all");
        return "/api/files/doctors/" + doctorId + "/documents";
    }

    /**
     * Helper method to construct specific document URL
     */
    private String constructDocumentUrl(Long doctorId, String documentType) {
        // This should return the actual document storage URL
        // Example: return fileStorageService.getDocumentUrl(doctorId, documentType);
        return "/api/files/doctors/" + doctorId + "/" + documentType;
    }

    /**
     * Verify Doctor - Approve or Reject
     * Called by admin-service via Feign
     */
    @Transactional
    public DoctorVerificationResponseDto verifyDoctor(Long doctorId, Boolean approved, String reason, String notes) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BusinessException(
                        "Doctor not found with ID: " + doctorId,
                        HttpStatus.NOT_FOUND
                ));

        // Validate current status
        if (doctor.getVerificationStatus() != VerificationStatus.PENDING) {
            throw new BusinessException(
                    "Doctor verification status is already " + doctor.getVerificationStatus(),
                    HttpStatus.BAD_REQUEST
            );
        }

        if (approved) {
            // Approve doctor
            doctor.setVerificationStatus(VerificationStatus.VERIFIED);
            doctor.setVerifiedAt(LocalDateTime.now());
            doctor.setVerificationNotes(notes);
            doctor.setRejectionReason(null); // Clear any previous rejection
            doctor.setIsAvailable(true);

            log.info("Doctor {} has been verified and approved", doctorId);

        } else {
            // Reject doctor
            if (reason == null || reason.trim().isEmpty()) {
                throw new BusinessException(
                        "Rejection reason is required",
                        HttpStatus.BAD_REQUEST
                );
            }

            doctor.setVerificationStatus(VerificationStatus.REJECTED);
            doctor.setRejectionReason(reason);
            doctor.setVerificationNotes(notes);
            doctor.setIsAvailable(false);

            log.info("Doctor {} has been rejected. Reason: {}", doctorId, reason);
        }

        doctor = doctorRepository.save(doctor);

        // TODO: Send notification to doctor via Kafka
        //doctorEventProducer.publishDoctorVerificationEvent(doctor, approved);

        return DoctorVerificationResponseDto.builder()
                .doctorId(doctor.getId())
                .fullName(doctor.getFullName())
                .doctorEmail(doctor.getEmail())
                .status(doctor.getVerificationStatus().name())
                .message(approved ? "Doctor verified successfully" : "Doctor verification rejected")
                .processedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Reject Doctor - Convenience method for rejection only
     */
    @Transactional
    public DoctorVerificationResponseDto rejectDoctor(Long doctorId, String reason, String additionalNotes) {
        return verifyDoctor(doctorId, false, reason, additionalNotes);
    }

    /**
     * Update Doctor Status (Active/Inactive/Suspended)
     * Different from verification status
     */
    @Transactional
    public void updateDoctorStatus(Long doctorId, String status, String reason) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BusinessException(
                        "Doctor not found with ID: " + doctorId,
                        HttpStatus.NOT_FOUND
                ));

        // Validate doctor is verified before changing availability status
        if (doctor.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new BusinessException(
                    "Cannot update status for unverified doctor",
                    HttpStatus.BAD_REQUEST
            );
        }

        switch (status.toUpperCase()) {
            case "ACTIVE":
                doctor.setIsAvailable(true);
                doctor.setEmergencyMode(false);
                log.info("Doctor {} set to ACTIVE", doctorId);
                break;

            case "INACTIVE":
                doctor.setIsAvailable(false);
                doctor.setEmergencyMode(false);
                log.info("Doctor {} set to INACTIVE. Reason: {}", doctorId, reason);
                break;

            case "SUSPENDED":
                doctor.setIsAvailable(false);
                doctor.setEmergencyMode(false);
                // You might want to add a 'suspensionReason' field to Doctor entity
                log.warn("Doctor {} SUSPENDED. Reason: {}", doctorId, reason);
                break;

            default:
                throw new BusinessException(
                        "Invalid status: " + status + ". Valid values: ACTIVE, INACTIVE, SUSPENDED",
                        HttpStatus.BAD_REQUEST
                );
        }

        doctorRepository.save(doctor);

        // TODO: Send notification to doctor
    }

    /**
     * Get All Doctors with Filters and Pagination
     */
    public Page<DoctorSummaryDto> getAllDoctors(DoctorFilterDto filters, Pageable pageable) {
        Page<Doctor> doctorPage;

        // Build specification based on filters
        if (hasFilters(filters)) {
            Specification<Doctor> spec = buildDoctorSpecification(filters);
            doctorPage = doctorRepository.findAll(spec, pageable);
        } else {
            doctorPage = doctorRepository.findAll(pageable);
        }

        return doctorPage.map(this::mapToDoctorSummaryDto);
    }

    /**
     * Helper: Check if any filters are applied
     */
    private boolean hasFilters(DoctorFilterDto filters) {
        return filters.getSearchTerm() != null ||
                filters.getVerificationStatus() != null ||
                filters.getSpecialization() != null ||
                filters.getIsAvailable() != null ||
                filters.getEmergencyMode() != null ||
                filters.getMinYearsExperience() != null ||
                filters.getMaxYearsExperience() != null ||
                filters.getMinRating() != null ||
                filters.getCity() != null ||
                filters.getCountry() != null;
    }

    /**
     * Helper: Build JPA Specification for filtering
     */
    private Specification<Doctor> buildDoctorSpecification(DoctorFilterDto filters) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search term (name, license, email)
            if (filters.getSearchTerm() != null && !filters.getSearchTerm().isEmpty()) {
                String searchPattern = "%" + filters.getSearchTerm().toLowerCase() + "%";
                Predicate namePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("fullName")), searchPattern);
                Predicate licensePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("licenseNumber")), searchPattern);
                Predicate emailPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("email")), searchPattern);

                predicates.add(criteriaBuilder.or(namePredicate, licensePredicate, emailPredicate));
            }

            // Verification status
            if (filters.getVerificationStatus() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("verificationStatus"), filters.getVerificationStatus()));
            }

            // Specialization
            if (filters.getSpecialization() != null && !filters.getSpecialization().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                        root.get("primarySpecialization"), filters.getSpecialization()));
            }

            // Availability
            if (filters.getIsAvailable() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("isAvailable"), filters.getIsAvailable()));
            }

            // Emergency mode
            if (filters.getEmergencyMode() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("emergencyMode"), filters.getEmergencyMode()));
            }

            // Years of experience range
            if (filters.getMinYearsExperience() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("yearsOfExperience"), filters.getMinYearsExperience()));
            }
            if (filters.getMaxYearsExperience() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("yearsOfExperience"), filters.getMaxYearsExperience()));
            }

            // Minimum rating
            if (filters.getMinRating() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("rating"), filters.getMinRating()));
            }

            // Location filters
            if (filters.getCity() != null && !filters.getCity().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("city")),
                        filters.getCity().toLowerCase()));
            }
            if (filters.getCountry() != null && !filters.getCountry().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("country")),
                        filters.getCountry().toLowerCase()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Helper: Map Doctor entity to DoctorSummaryDto
     */
    private DoctorSummaryDto mapToDoctorSummaryDto(Doctor doctor) {
        return DoctorSummaryDto.builder()
                .id(doctor.getId())
                .userId(doctor.getUserId())
                .fullName(doctor.getFullName())
                .email(doctor.getEmail())
                .phoneNumber(doctor.getPhoneNumber())
                .licenseNumber(doctor.getLicenseNumber())
                .primarySpecialization(doctor.getPrimarySpecialization())
                .subSpecializations(doctor.getSubSpecializations())
                .verificationStatus(doctor.getVerificationStatus().name())
                .isAvailable(doctor.getIsAvailable())
                .emergencyMode(doctor.getEmergencyMode())
                .yearsOfExperience(doctor.getYearsOfExperience())
                .consultationCount(doctor.getConsultationCount())
                .rating(doctor.getRating())
                .totalRatings(doctor.getTotalRatings())
                .activeCases(doctor.getActiveCases())
                .workloadPercentage(doctor.getWorkloadPercentage())
                .city(doctor.getCity())
                .country(doctor.getCountry())
                .hospitalAffiliation(doctor.getHospitalAffiliation())
                .createdAt(doctor.getCreatedAt())
                .verifiedAt(doctor.getVerifiedAt())
                .build();
    }

}