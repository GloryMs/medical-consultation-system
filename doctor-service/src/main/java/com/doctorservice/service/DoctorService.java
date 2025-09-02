package com.doctorservice.service;

import com.commonlibrary.dto.DoctorProfileDto;
import com.commonlibrary.entity.AppointmentStatus;
import com.commonlibrary.entity.CaseStatus;
import com.commonlibrary.entity.VerificationStatus;
import com.commonlibrary.exception.BusinessException;
import com.doctorservice.dto.*;
import com.doctorservice.entity.*;
import com.doctorservice.repository.AppointmentRepository;
import com.doctorservice.repository.CalendarAvailabilityRepository;
import com.doctorservice.repository.ConsultationReportRepository;
import com.doctorservice.repository.DoctorRepository;
import com.doctorservice.feign.PatientServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
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
                .primarySpecialization(dto.getPrimarySpecializationCode())
                .subSpecializations(dto.getSubSpecializationCodes())
                .caseRate(dto.getCaseRate())
                .hourlyRate(dto.getHourlyRate())
                .verificationStatus(VerificationStatus.PENDING)
                .emergencyRate(dto.getComplexCaseFee())
                .yearsOfExperience(dto.getYearsOfExperience())
                .rating(0.0)
                .yearsOfExperience(dto.getYearsOfExperience())
                .phoneNumber(dto.getPhoneNumber())
                .email(dto.getEmail())
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

    public List<Appointment> getPatientAppointments(Long patientId) {
        List<Appointment> patientAppointments = new ArrayList<>();
        patientAppointments = appointmentRepository.findByPatientId(patientId);
        return patientAppointments;
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

        /*TODO
        *  Update doctor's consultation count*/

        // Update doctor's consultation count
        //doctor.setConsultationCount(doctor.getConsultationCount() + 1);
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
//        dto.setPrimarySpecialization(doctor.getPrimarySpecialization());
//        dto.setSubSpecialization(doctor.getSubSpecialization());
//        dto.setHourlyRate(doctor.getHourlyRate());
//        dto.setCaseRate(doctor.getCaseRate());
        dto.setVerificationStatus(doctor.getVerificationStatus());
        //dto.setProfessionalSummary(doctor.getProfessionalSummary());
        dto.setYearsOfExperience(doctor.getYearsOfExperience());
        //dto.setPhoneNumber(doctor.getPhoneNumber());
        //dto.setEmail(doctor.getEmail());
        //dto.setHospitalAffiliation(doctor.getHospitalAffiliation());
        //dto.setQualifications(doctor.getQualifications());
        //dto.setLanguages(doctor.getLanguages());
        return dto;
    }

    // 10. Get Profile Implementation
    public DoctorProfileDto getProfile(Long userId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));
        return mapToDto(doctor);
    }

    /*TODO
    *  updateAvailability not works now, must be fixed*/

    // 11. Update Availability Implementation
    @Transactional
    public void updateAvailability(Long userId, AvailabilityDto dto) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        //doctor.setAvailableTimeSlots(dto.getAvailableTimeSlots());
        doctor.setIsAvailable(dto.getIsAvailable());
        doctorRepository.save(doctor);
    }

    // 12. Get Assigned Cases Implementation
    public List<CaseDto> getAssignedCases(Long userId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        return patientServiceClient.getCasesByDoctorId(doctor.getId()).getBody().getData();
    }

    public List<CaseDto> browseCasesPool(Long userId, String specialization) {
        List<CaseDto> cases = new ArrayList<>();
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        String spec = specialization != null ? specialization : doctor.getPrimarySpecialization();
        try{
            cases = patientServiceClient.getCasesPool(spec).getBody().getData();
        } catch (Exception e) {
            log.error("Error fetching cases based on doctor specialization");
            log.error(e.getMessage());
        }
        return cases;
    }

    // 15. Reject Case Implementation
    @Transactional
    public void rejectCase(Long userId, Long caseId, String reason) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        /*TODO
        *  I think no more need for below - double check*/
        //patientServiceClient.rejectCase(caseId, doctor.getId(), reason);
    }

    // 16. Set Dynamic Fee Implementation
    @Transactional
    public void setDynamicFee(Long userId, Long caseId, SetFeeDto dto) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        /*TODO
         *  I think no more need for below - double check*/
        //patientServiceClient.setCaseFee(caseId, dto.getConsultationFee(), dto.getReason());
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

//    @Transactional
//    public void updateDoctorWorkLoad(Long doctorId, CaseStatus status, int flag) {
//        Doctor doctor = doctorRepository.findById(doctorId)
//                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));
//
//        if(flag == 1){ //increase work load
//            //PENDING
//            if( status.name().equals("ASSIGNED") ) {
//                doctor.setCurrentCaseLoad( doctor.getCurrentCaseLoad() + 1 );
//            }
//            else if (status.name().equals("ACCEPTED")) {
//                doctor.setAcceptedCases( doctor.getAcceptedCases() + 1 );
//            }
//        }
//        else{ //decrease work load
//            doctor.setCurrentCaseLoad( Math.max(doctor.getCurrentCaseLoad() - 1 , 0)  );
//        }
//        doctorRepository.save(doctor);
//    }

//    @Transactional
//    public void updateDoctorWorkload(Long doctorId, Long caseId, String status) {
//        // Update doctor's case count and workload statistics
//        // This replaces the database trigger functionality
//        //log.info("Updating doctor {} workload for case {} with status {}", doctorId, caseId, status);
//
//        // Implement your workload update logic here
//        // Example: Update active cases count, completion statistics, etc.
//    }

    @Transactional
    public void initializeDoctorProfile(Long userId, String email) {
        // Check if doctor profile already exists
        if (doctorRepository.findByUserId(userId).isPresent()) {
            log.info("Doctor profile already exists for user: {}", userId);
            return;
        }

        // Create basic doctor profile
        Doctor doctor = Doctor.builder()
                .userId(userId)
                .email(email)
                .verificationStatus(VerificationStatus.PENDING)
                .isAvailable(false)
                .consultationCount(0)
                .rating(0.0)
                .activeCases(0)
                .workloadPercentage(0.0)
                .build();

        doctorRepository.save(doctor);
        //log.info("Doctor profile initialized for user: {}", userId);
    }
}
