package com.doctorservice.service;

import com.commonlibrary.exception.BusinessException;
import com.doctorservice.dto.*;
import com.doctorservice.entity.*;
import com.doctorservice.repository.AppointmentRepository;
import com.doctorservice.repository.CalendarAvailabilityRepository;
import com.doctorservice.repository.ConsultationReportRepository;
import com.doctorservice.repository.DoctorRepository;
import com.doctorservice.feign.PatientServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientServiceClient patientServiceClient;
    private final ConsultationReportRepository consultationReportRepository;
    private final CalendarAvailabilityRepository calendarAvailabilityRepository;

    @Transactional
    public DoctorProfileDto createProfile(Long userId, DoctorProfileDto dto) {
        if (doctorRepository.existsByUserId(userId)) {
            throw new BusinessException("Doctor profile already exists", HttpStatus.CONFLICT);
        }

        if (doctorRepository.existsByLicenseNumber(dto.getLicenseNumber())) {
            throw new BusinessException("License number already registered", HttpStatus.CONFLICT);
        }

        Doctor doctor = Doctor.builder()
                .userId(userId)
                .fullName(dto.getFullName())
                .licenseNumber(dto.getLicenseNumber())
                .primarySpecialization(dto.getPrimarySpecialization())
                .subSpecialization(dto.getSubSpecialization())
                .hourlyRate(dto.getHourlyRate())
                .caseRate(dto.getCaseRate())
                .verificationStatus(VerificationStatus.PENDING)
                .professionalSummary(dto.getProfessionalSummary())
                .yearsOfExperience(dto.getYearsOfExperience())
                .phoneNumber(dto.getPhoneNumber())
                .email(dto.getEmail())
                .hospitalAffiliation(dto.getHospitalAffiliation())
                .qualifications(dto.getQualifications())
                .languages(dto.getLanguages())
                .averageRating(0.0)
                .consultationCount(0)
                .isAvailable(true)
                .build();

        Doctor saved = doctorRepository.save(doctor);
        return mapToDto(saved);
    }

    @Transactional
    public void acceptCase(Long userId, Long caseId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        if (doctor.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new BusinessException("Doctor profile not verified", HttpStatus.FORBIDDEN);
        }

        // Update case status through patient service
        patientServiceClient.updateCaseStatus(caseId, "ACCEPTED", doctor.getId());
    }

    @Transactional
    public Appointment scheduleAppointment(Long userId, ScheduleAppointmentDto dto) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        Appointment appointment = Appointment.builder()
                .caseId(dto.getCaseId())
                .doctor(doctor)
                .patientId(dto.getPatientId())
                .scheduledTime(dto.getScheduledTime())
                .duration(dto.getDuration())
                .consultationType(dto.getConsultationType())
                .status(AppointmentStatus.SCHEDULED)
                .rescheduleCount(0)
                .build();

        // Generate meeting link based on consultation type
        generateMeetingLink(appointment);

        Appointment saved = appointmentRepository.save(appointment);

        // Update case status to SCHEDULED
        patientServiceClient.updateCaseStatus(dto.getCaseId(), "SCHEDULED", doctor.getId());

        return saved;
    }

    public List<Appointment> getDoctorAppointments(Long userId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        return appointmentRepository.findByDoctorId(doctor.getId());
    }

    @Transactional
    public ConsultationReport createConsultationReport(Long userId, ConsultationReportDto dto) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        Appointment appointment = appointmentRepository.findById(dto.getAppointmentId())
                .orElseThrow(() -> new BusinessException("Appointment not found", HttpStatus.NOT_FOUND));

        if (!appointment.getDoctor().getId().equals(doctor.getId())) {
            throw new BusinessException("Unauthorized access to appointment", HttpStatus.FORBIDDEN);
        }

        ConsultationReport report = ConsultationReport.builder()
                .appointment(appointment)
                .doctor(doctor)
                .caseId(dto.getCaseId())
                .diagnosis(dto.getDiagnosis())
                .recommendations(dto.getRecommendations())
                .prescriptions(dto.getPrescriptions())
                .followUpInstructions(dto.getFollowUpInstructions())
                .requiresFollowUp(dto.getRequiresFollowUp())
                .nextAppointmentSuggested(dto.getNextAppointmentSuggested())
                .doctorNotes(dto.getDoctorNotes())
                .build();

        // Update appointment status
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);

        // Update case status to CONSULTATION_COMPLETE
        patientServiceClient.updateCaseStatus(dto.getCaseId(), "CONSULTATION_COMPLETE", doctor.getId());

        // Update doctor's consultation count
        doctor.setConsultationCount(doctor.getConsultationCount() + 1);
        doctorRepository.save(doctor);

        return report;
    }

    private void generateMeetingLink(Appointment appointment) {
        switch (appointment.getConsultationType()) {
            case ZOOM:
                appointment.setMeetingLink("https://zoom.us/j/" + System.currentTimeMillis());
                appointment.setMeetingId("ZOOM_" + System.currentTimeMillis());
                break;
            case WHATSAPP:
                appointment.setMeetingLink("https://wa.me/" + System.currentTimeMillis());
                appointment.setMeetingId("WA_" + System.currentTimeMillis());
                break;
            default:
                appointment.setMeetingId("MEETING_" + System.currentTimeMillis());
        }
    }

    private DoctorProfileDto mapToDto(Doctor doctor) {
        DoctorProfileDto dto = new DoctorProfileDto();
        dto.setId(doctor.getId());
        dto.setUserId(doctor.getUserId());
        dto.setFullName(doctor.getFullName());
        dto.setLicenseNumber(doctor.getLicenseNumber());
        dto.setPrimarySpecialization(doctor.getPrimarySpecialization());
        dto.setSubSpecialization(doctor.getSubSpecialization());
        dto.setHourlyRate(doctor.getHourlyRate());
        dto.setCaseRate(doctor.getCaseRate());
        dto.setVerificationStatus(doctor.getVerificationStatus());
        dto.setProfessionalSummary(doctor.getProfessionalSummary());
        dto.setYearsOfExperience(doctor.getYearsOfExperience());
        dto.setPhoneNumber(doctor.getPhoneNumber());
        dto.setEmail(doctor.getEmail());
        dto.setHospitalAffiliation(doctor.getHospitalAffiliation());
        dto.setQualifications(doctor.getQualifications());
        dto.setLanguages(doctor.getLanguages());
        return dto;
    }

    // 10. Get Profile Implementation
    public DoctorProfileDto getProfile(Long userId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));
        return mapToDto(doctor);
    }

    // 11. Update Availability Implementation
    @Transactional
    public void updateAvailability(Long userId, AvailabilityDto dto) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        doctor.setAvailableTimeSlots(dto.getAvailableTimeSlots());
        doctor.setIsAvailable(dto.getIsAvailable());
        doctorRepository.save(doctor);
    }

    // 12. Get Assigned Cases Implementation
    public List<CaseDto> getAssignedCases(Long userId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        return patientServiceClient.getCasesByDoctorId(doctor.getId());
    }

    // 13. Browse Cases Pool Implementation
    public List<CaseDto> browseCasesPool(Long userId, String specialization) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        String spec = specialization != null ? specialization : doctor.getPrimarySpecialization();
        return patientServiceClient.getCasesPool(spec);
    }

    // 15. Reject Case Implementation
    @Transactional
    public void rejectCase(Long userId, Long caseId, String reason) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        patientServiceClient.rejectCase(caseId, doctor.getId(), reason);
    }

    // 16. Set Dynamic Fee Implementation
    @Transactional
    public void setDynamicFee(Long userId, Long caseId, SetFeeDto dto) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        patientServiceClient.setCaseFee(caseId, dto.getConsultationFee(), dto.getReason());
    }

    // 17. Reschedule Appointment Implementation
    @Transactional
    public void rescheduleAppointment(Long userId, Long appointmentId, RescheduleDto dto) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException("Appointment not found", HttpStatus.NOT_FOUND));

        if (!appointment.getDoctor().getId().equals(doctor.getId())) {
            throw new BusinessException("Unauthorized access", HttpStatus.FORBIDDEN);
        }

        appointment.setRescheduledFrom(appointment.getScheduledTime());
        appointment.setScheduledTime(dto.getScheduledTime());
        appointment.setRescheduleReason(dto.getReason());
        appointment.setRescheduleCount(appointment.getRescheduleCount() + 1);
        appointment.setStatus(AppointmentStatus.RESCHEDULED);

        appointmentRepository.save(appointment);
    }

    // 18. Get Today's Appointments Implementation
    public List<Appointment> getTodayAppointments(Long userId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        return appointmentRepository.findByDoctorIdAndScheduledTimeBetween(
                doctor.getId(), startOfDay, endOfDay);
    }

    // 19. Cancel Appointment Implementation
    @Transactional
    public void cancelAppointment(Long userId, Long appointmentId, String reason) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException("Appointment not found", HttpStatus.NOT_FOUND));

        if (!appointment.getDoctor().getId().equals(doctor.getId())) {
            throw new BusinessException("Unauthorized access", HttpStatus.FORBIDDEN);
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setRescheduleReason(reason);
        appointmentRepository.save(appointment);
    }

    // 20. Get Consultation Reports Implementation
    public List<ConsultationReport> getConsultationReports(Long userId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        return consultationReportRepository.findByDoctorId(doctor.getId());
    }

    // 21. Update Consultation Report Implementation
    @Transactional
    public ConsultationReport updateConsultationReport(Long userId, Long reportId, UpdateReportDto dto) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        ConsultationReport report = consultationReportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException("Report not found", HttpStatus.NOT_FOUND));

        if (!report.getDoctor().getId().equals(doctor.getId())) {
            throw new BusinessException("Unauthorized access", HttpStatus.FORBIDDEN);
        }

        if (dto.getDoctorNotes() != null) {
            report.setDoctorNotes(report.getDoctorNotes() + "\n\nAddendum: " + dto.getDoctorNotes());
        }

        return consultationReportRepository.save(report);
    }

    // 22. Close Case Implementation
    @Transactional
    public void closeCase(Long userId, Long caseId, String closureNotes) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        patientServiceClient.closeCase(caseId, doctor.getId(), closureNotes);
    }

    // 23. Set Calendar Availability Implementation
    @Transactional
    public CalendarAvailability setCalendarAvailability(Long userId, CalendarAvailabilityDto dto) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        CalendarAvailability availability = CalendarAvailability.builder()
                .doctor(doctor)
                .availableDate(dto.getAvailableDate())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .isRecurring(dto.getIsRecurring())
                .recurrencePattern(dto.getRecurrencePattern())
                .isBlocked(false)
                .build();

        return calendarAvailabilityRepository.save(availability);
    }

    // 24. Block Time Slot Implementation
    @Transactional
    public void blockTimeSlot(Long userId, BlockTimeSlotDto dto) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        CalendarAvailability block = CalendarAvailability.builder()
                .doctor(doctor)
                .availableDate(dto.getAvailableDate())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .isBlocked(true)
                .blockReason(dto.getBlockReason())
                .build();

        calendarAvailabilityRepository.save(block);
    }

    // 14. Update Verification Status (for admin use)
    @Transactional
    public void updateVerificationStatus(Long doctorId, String status, String reason) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        doctor.setVerificationStatus(VerificationStatus.valueOf(status));
        if (status.equals("VERIFIED")) {
            doctor.setVerifiedAt(LocalDateTime.now());
        }

        doctorRepository.save(doctor);
    }
}
