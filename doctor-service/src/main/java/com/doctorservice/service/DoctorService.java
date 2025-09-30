package com.doctorservice.service;

import com.commonlibrary.dto.*;
import com.commonlibrary.entity.*;
import com.commonlibrary.exception.BusinessException;
import com.doctorservice.dto.*;
import com.doctorservice.entity.*;
import com.doctorservice.feign.NotificationServiceClient;
import com.doctorservice.feign.PaymentServiceClient;
import com.doctorservice.kafka.DoctorEventProducer;
import com.doctorservice.repository.AppointmentRepository;
import com.doctorservice.repository.CalendarAvailabilityRepository;
import com.doctorservice.repository.ConsultationReportRepository;
import com.doctorservice.repository.DoctorRepository;
import com.doctorservice.feign.PatientServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientServiceClient patientServiceClient;
    private final ConsultationReportRepository consultationReportRepository;
    private final CalendarAvailabilityRepository calendarAvailabilityRepository;
    private final DoctorEventProducer doctorEventProducer;
    private final NotificationServiceClient notificationServiceClient;
    private final PaymentServiceClient paymentServiceClient;

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
                .emergencyRate(dto.getEmergencyRate())
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

    /**
     * Update Doctor Profile Implementation
     */
    @Transactional
    public DoctorProfileDto updateProfile(Long userId, DoctorProfileDto dto) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        // Update basic information
        if (dto.getFullName() != null && !dto.getFullName().trim().isEmpty()) {
            doctor.setFullName(dto.getFullName().trim());
        }

        if (dto.getLicenseNumber() != null && !dto.getLicenseNumber().trim().isEmpty()) {
            // Check if license number is unique (excluding current doctor)
            boolean licenseExists = doctorRepository.existsByLicenseNumber(dto.getLicenseNumber())
                    && !doctor.getLicenseNumber().equals(dto.getLicenseNumber());
            if (licenseExists) {
                throw new BusinessException("License number already exists", HttpStatus.CONFLICT);
            }
            doctor.setLicenseNumber(dto.getLicenseNumber().trim());
        }

        // Update specialization information
        if (dto.getPrimarySpecializationCode() != null && !dto.getPrimarySpecializationCode().trim().isEmpty()) {
            doctor.setPrimarySpecialization(dto.getPrimarySpecializationCode().trim());
        }

        if (dto.getSubSpecializationCodes() != null && !dto.getSubSpecializationCodes().isEmpty()) {
            doctor.setSubSpecializations(new HashSet<>(dto.getSubSpecializationCodes()));
        }

        // Update professional information
        if (dto.getYearsOfExperience() != null && dto.getYearsOfExperience() >= 0) {
            doctor.setYearsOfExperience(dto.getYearsOfExperience());
        }

        if (dto.getQualifications() != null && !dto.getQualifications().trim().isEmpty()) {
            // Parse comma-separated qualifications into Set
            Set<String> qualificationSet = new HashSet<>();
            String[] qualificationArray = dto.getQualifications().split(",");
            for (String qual : qualificationArray) {
                if (!qual.trim().isEmpty()) {
                    qualificationSet.add(qual.trim());
                }
            }
            doctor.setQualifications(qualificationSet);
        }

        if (dto.getLanguages() != null && !dto.getLanguages().trim().isEmpty()) {
            // Parse comma-separated languages into Set
            Set<String> languageSet = new HashSet<>();
            String[] languageArray = dto.getLanguages().split(",");
            for (String lang : languageArray) {
                if (!lang.trim().isEmpty()) {
                    languageSet.add(lang.trim());
                }
            }
            doctor.setLanguages(languageSet);
        }

        // Update pricing information
        if (dto.getBaseConsultationFee() != null && dto.getBaseConsultationFee() >= 0) {
            doctor.setCaseRate(dto.getBaseConsultationFee());
        }

        if (dto.getHourlyRate() != null && dto.getHourlyRate() >= 0) {
            doctor.setHourlyRate(dto.getHourlyRate());
        }

        if (dto.getUrgentCaseFee() != null && dto.getUrgentCaseFee() >= 0) {
            doctor.setEmergencyRate(dto.getUrgentCaseFee());
        }

        // Update contact information
        if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().trim().isEmpty()) {
            doctor.setPhoneNumber(dto.getPhoneNumber().trim());
        }

        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            doctor.setEmail(dto.getEmail().trim());
        }

        if (dto.getHospitalAffiliation() != null && !dto.getHospitalAffiliation().trim().isEmpty()) {
            doctor.setHospitalAffiliation(dto.getHospitalAffiliation().trim());
        }

        // Update capacity settings
        if (dto.getMaxConcurrentCases() != null && dto.getMaxConcurrentCases() > 0) {
            doctor.setMaxActiveCases(dto.getMaxConcurrentCases());
        }

        // Update preferences
        if (dto.getAcceptsSecondOpinions() != null) {
            // Note: This field doesn't exist in current Doctor entity, but if added later
            // doctor.setAcceptsSecondOpinions(dto.getAcceptsSecondOpinions());
        }

        if (dto.getAcceptsComplexCases() != null) {
            // Note: This field doesn't exist in current Doctor entity, but if added later
            // doctor.setAcceptsComplexCases(dto.getAcceptsComplexCases());
        }

        if (dto.getAcceptsUrgentCases() != null) {
            // Note: This field doesn't exist in current Doctor entity, but if added later
            // doctor.setAcceptsUrgentCases(dto.getAcceptsUrgentCases());
        }

        // Save updated doctor
        Doctor updatedDoctor = doctorRepository.save(doctor);

        // Return updated profile DTO
        return mapToDto(updatedDoctor);
    }

    public CustomDoctorInfoDto getDoctorCustomInfo(Long doctorId){
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));
        CustomDoctorInfoDto doctorInfoDto = new CustomDoctorInfoDto();
        doctorInfoDto.setId(doctor.getId());
        doctorInfoDto.setUserId(doctor.getUserId());
        doctorInfoDto.setFullName(doctor.getFullName());

        return doctorInfoDto;
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

    /**
     * Set consultation fee for a case
     * Only allowed when doctor has no hourlyRate or caseRate set in profile
     */
    @Transactional
    public void setCaseFee(Long userId, Long caseId, BigDecimal consultationFee) {
        // Find doctor
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        // Check if doctor has hourlyRate or caseRate already set
        if (doctor.getHourlyRate() != null || doctor.getCaseRate() != null) {
            throw new BusinessException("Cannot set case-specific fee when hourlyRate or caseRate is already set in profile",
                    HttpStatus.BAD_REQUEST);
        }

        // Verify doctor is assigned to this case
        try {
            List<CaseDto> assignedCases = patientServiceClient.getDoctorActiveCases(doctor.getId()).getBody().getData();
            CaseDto targetCase = assignedCases.stream()
                    .filter(c -> c.getId().equals(caseId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("Case not found or not assigned to doctor", HttpStatus.NOT_FOUND));

            // Check if case is in correct status (ACCEPTED)
            if (targetCase.getStatus() != CaseStatus.ACCEPTED) {
                throw new BusinessException("Can only set fee for accepted cases. Current status: " + targetCase.getStatus(),
                        HttpStatus.BAD_REQUEST);
            }

            // Check if fee already set
            if (targetCase.getConsultationFee() != null) {
                throw new BusinessException("Consultation fee has already been set for this case", HttpStatus.CONFLICT);
            }

            // Get patient ID from case (you might need to add this to CaseDto)
            CustomPatientDto patientDto = getCustomPatientInfo(caseId, userId); // Helper method

            // Send Kafka event to update case and notify patient
            doctorEventProducer.sendCaseFeeUpdateEvent(caseId, doctor.getId(), doctor.getUserId(),
                    patientDto.getId(), patientDto.getUserId(),  consultationFee);

            log.info("Case fee set successfully for case {} by doctor {}. Fee: ${}",
                    caseId, doctor.getId(), consultationFee);

        } catch (Exception e) {
            log.error("Error setting case fee for case {}: {}", caseId, e.getMessage());
            throw new BusinessException("Failed to set consultation fee: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * Helper method to get patient ID from case
     */
    public CustomPatientDto getCustomPatientInfo(Long caseId, Long userId) {
        try {
            Doctor doctor = doctorRepository.findByUserId(userId)
                    .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));
            // Call patient service to get case details
            CustomPatientDto patientDto = patientServiceClient.getCustomPatientInfo(caseId, doctor.getId()).getBody().getData();
            return patientDto; // You might need to add this field to CaseDto
        } catch (Exception e) {
            log.error("Error getting patient custom info for case {}: {}", caseId, e.getMessage());
            e.printStackTrace();
            throw new BusinessException("Error getting patient custom info", HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
        patientServiceClient.updateCaseStatus(dto.getCaseId(), "PAYMENT_PENDING", doctor.getId()); // Was SCHEDULED

        //Send notification to Patient:
        doctorEventProducer.SendCaseScheduleUpdate(dto.getPatientId(), dto.getCaseId(),
                dto.getScheduledTime(), doctor.getFullName());

        return saved;
    }

    public List<AppointmentDto> getDoctorAppointments(Long userId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        return appointmentRepository.findByDoctorId(doctor.getId()).stream().map(this::convertToAppointmentDto).toList();
    }

    public List<AppointmentDto> getPatientAppointments(Long patientId) {
        List<AppointmentDto> patientAppointments = new ArrayList<>();
        patientAppointments = appointmentRepository.findByPatientId(patientId).
                stream().map(this::convertToAppointmentDto).toList();
        return patientAppointments;
    }

    public AppointmentDto convertToAppointmentDto(Appointment appointment) {
        AppointmentDto appointmentDto = new AppointmentDto();
        ModelMapper modelMapper = new ModelMapper();
        appointmentDto = modelMapper.map(appointment, AppointmentDto.class);
        return appointmentDto;
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
        dto.setPrimarySpecializationCode(doctor.getPrimarySpecialization());

        // Convert sets to comma-separated strings for frontend compatibility
        if (doctor.getSubSpecializations() != null && !doctor.getSubSpecializations().isEmpty()) {
            dto.setSubSpecializationCodes(doctor.getSubSpecializations());
        }

        if (doctor.getQualifications() != null && !doctor.getQualifications().isEmpty()) {
            dto.setQualifications(String.join(", ", doctor.getQualifications()));
        }

        if (doctor.getLanguages() != null && !doctor.getLanguages().isEmpty()) {
            dto.setLanguages(String.join(", ", doctor.getLanguages()));
        }

        dto.setVerificationStatus(doctor.getVerificationStatus());
        dto.setYearsOfExperience(doctor.getYearsOfExperience());
        dto.setPhoneNumber(doctor.getPhoneNumber());
        dto.setEmail(doctor.getEmail());
        dto.setHospitalAffiliation(doctor.getHospitalAffiliation());

        // Convert pricing fields
        if (doctor.getCaseRate() != null) {
            dto.setBaseConsultationFee(doctor.getCaseRate());
            dto.setCaseRate(doctor.getCaseRate());
        }

        if (doctor.getHourlyRate() != null) {
            dto.setHourlyRate(doctor.getHourlyRate());
        }

        if (doctor.getEmergencyRate() != null) {
            dto.setUrgentCaseFee(doctor.getEmergencyRate());
            dto.setEmergencyRate(doctor.getEmergencyRate());
        }

        // Capacity settings
        if (doctor.getMaxActiveCases() != null) {
            dto.setMaxConcurrentCases(doctor.getMaxActiveCases());
        }


        return dto;
    }

    // 10. Get Profile Implementation
    public DoctorProfileDto getProfile(Long userId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));
        return mapToDto(doctor);
    }

    // 11. Update Availability Implementation
    /**
     * Update doctor availability including time slots, emergency mode, and capacity settings
     * This method handles both simple availability toggle and complex schedule updates
     */
    @Transactional
    public void updateAvailability(Long userId, AvailabilityDto dto) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        log.info("Updating availability for doctor {} (userId: {}). New status: {}",
                doctor.getId(), userId, dto.getIsAvailable());

        // Store previous state for logging
        Boolean previousAvailability = doctor.getIsAvailable();
        Boolean previousEmergencyMode = doctor.getEmergencyMode();

        try {
            // 1. Update basic availability status
            doctor.setIsAvailable(dto.getIsAvailable());

            // 2. Handle Emergency Mode
            updateEmergencyMode(doctor, dto);

            // 3. Update Time Slots (if provided and not a quick toggle)
            if (!dto.getQuickToggle() && dto.getAvailableTimeSlots() != null) {
                updateAvailableTimeSlots(doctor, dto.getAvailableTimeSlots());
            }

            // 4. Update Capacity Settings (if provided)
            updateCapacitySettings(doctor, dto);

            // 5. Update workload based on new availability
            updateWorkloadBasedOnAvailability(doctor);

            // 6. Save the updated doctor
            Doctor savedDoctor = doctorRepository.save(doctor);

            // 7. Log the availability change
            logAvailabilityChange(savedDoctor, previousAvailability, previousEmergencyMode, dto);

            // 8. Send notifications if significant change
            handleAvailabilityNotifications(savedDoctor, previousAvailability, dto);

            log.info("Successfully updated availability for doctor {}", doctor.getId());

        } catch (Exception e) {
            log.error("Error updating availability for doctor {}: {}", doctor.getId(), e.getMessage(), e);
            throw new BusinessException("Failed to update availability: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get current availability status with detailed information
     */
    public AvailabilityStatusDto getAvailabilityStatus(Long userId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        AvailabilityStatusDto status = new AvailabilityStatusDto();
        status.setDoctorId(doctor.getId());
        status.setIsAvailable(doctor.getIsAvailable());
        status.setEmergencyMode(doctor.getEmergencyMode());
        status.setEmergencyReason(doctor.getEmergencyModeReason());
        status.setAvailableTimeSlots(doctor.getAvailableTimeSlots());
        status.setMaxActiveCases(doctor.getMaxActiveCases());
        status.setMaxDailyAppointments(doctor.getMaxDailyAppointments());
        status.setCurrentActiveCases(doctor.getActiveCases());
        status.setTodayAppointments(doctor.getTodayAppointments());
        status.setWorkloadPercentage(doctor.getWorkloadPercentage());
        status.setLastUpdated(doctor.getLastWorkloadUpdate());

        return status;
    }

    /**
     * Quick toggle availability (simple on/off without changing time slots)
     */
    @Transactional
    public void toggleAvailability(Long userId, Boolean isAvailable, String reason) {
        AvailabilityDto dto = new AvailabilityDto();
        dto.setIsAvailable(isAvailable);
        dto.setReason(reason);
        dto.setQuickToggle(true);

        updateAvailability(userId, dto);
    }

    // 12. Get Assigned Cases Implementation
    public List<CaseDto> getAssignedCases(Long userId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        List<CaseDto> assignedCases = new ArrayList<>();
        try{
            assignedCases = patientServiceClient.getNewAssignedCasesForDoctor(doctor.getId()).getBody().getData();
            System.out.println("Doctor-Service: Assigned cases for doctor: "+
                    doctor.getId() + " are: "+ assignedCases.size());
        }catch (Exception e){
            log.error("Failed to get assgined cases for doctor: " + doctor.getFullName() +
                    " - Id:" + doctor.getId(), e);
            e.printStackTrace();
        }
        return assignedCases;
    }

    public List<CaseDto> getActiveCases(Long userId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        List<CaseDto> activeCase = new ArrayList<>();
        try{
            activeCase = patientServiceClient.getDoctorActiveCases(doctor.getId()).getBody().getData();
            System.out.println("Doctor-Service: Active cases for doctor: "+
                    doctor.getId() + " are: "+ activeCase.size());
        }catch (Exception e){
            log.error("Failed to get Active cases for doctor: " + doctor.getFullName() +
                    " - Id:" + doctor.getId(), e);
            e.printStackTrace();
        }
        return activeCase;
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
//    @Transactional
//    public void rejectCase(Long userId, Long caseId, String reason) {
//        Doctor doctor = doctorRepository.findByUserId(userId)
//                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));
//
//        /*TODO
//        *  I think no more need for below - double check*/
//        //patientServiceClient.rejectCase(caseId, doctor.getId(), reason);
//    }

    // 16. Set Dynamic Fee Implementation
//    @Transactional
//    public void setDynamicFee(Long userId, Long caseId, SetFeeDto dto) {
//        Doctor doctor = doctorRepository.findByUserId(userId)
//                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));
//
//        /*TODO
//         *  I think no more need for below - double check*/
//        patientServiceClient.setCaseFee(caseId, dto.getConsultationFee(), dto.getReason());
//    }

    /**
     * Get comprehensive dashboard data for doctor
     */
    public DoctorDashboardDto getDoctorDashboard(Long userId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        // Build dashboard stats
        DoctorDashboardStatsDto stats = buildDashboardStats(doctor);

        // Get recent active cases (limit 3)
        List<DashboardCaseDto> recentCases = getRecentActiveCases(doctor.getId());

        // Get upcoming appointments (limit 3)
        List<DashboardAppointmentDto> upcomingAppointments = getUpcomingAppointments(doctor.getId());

        // Get recent notifications (limit 3)
        List<NotificationDto> recentNotifications = getMyNotifications(doctor.getId());

        return DoctorDashboardDto.builder()
                .stats(stats)
                .recentCases(recentCases)
                .upcomingAppointments(upcomingAppointments)
                .recentNotifications(recentNotifications)
                .build();
    }

    /**
     * Build dashboard statistics
     */
    private DoctorDashboardStatsDto buildDashboardStats(Doctor doctor) {
        // Get active cases count from patient service
        Integer activeCases = 0;
        try {
            var response = patientServiceClient.getDoctorActiveCases(doctor.getId()).getBody().getData();
            if (response != null) {
                activeCases = response.size();
            }
        } catch (Exception e) {
            log.warn("Failed to get active cases count for doctor {}: {}", doctor.getId(), e.getMessage());
            activeCases = doctor.getActiveCases() != null ? doctor.getActiveCases() : 0;
        }

//        // Get today's appointments count
//        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
//        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
//        Integer todayAppointments = appointmentRepository
//                .findByDoctorIdAndScheduledTimeBetween(doctor.getId(), startOfDay, endOfDay)
//                .size();
        Integer todayAppointments = appointmentRepository.findByDoctorId(doctor.getId()).size();

        // Get pending reports count
        Integer pendingReports = (int) consultationReportRepository
                .findByDoctorId(doctor.getId())
                .stream()
                .filter(report -> report.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7)))
                .count();

        // Calculate this week's earnings (mock calculation - replace with actual payment service call)
        Double thisWeekEarnings = calculateTotalEarnings(doctor.getId());

        // Get unread notifications count (mock - replace with actual notification service)
        Integer unreadNotifications = getMyNotifications(doctor.getUserId()).size();; // This should come from notification service

        return DoctorDashboardStatsDto.builder()
                .activeCases(activeCases)
                .todayAppointments(todayAppointments)
                .totalConsultations(doctor.getConsultationCount())
                .avgRating(doctor.getRating())
                .workloadPercentage(doctor.getWorkloadPercentage())
                .totalEarnings(thisWeekEarnings)
                .pendingReports(pendingReports)
                .unreadNotifications(unreadNotifications)
                .build();
    }

    public List<NotificationDto> getMyNotifications(Long patientId){
        List<NotificationDto> dtos = new ArrayList<>();
        try{
            dtos = notificationServiceClient.getUserNotifications(patientId).getBody().getData().stream().
                    limit(3).collect(Collectors.toList());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return dtos;
    }

    /**
     * Get recent active cases for dashboard
     */
    private List<DashboardCaseDto> getRecentActiveCases(Long doctorId) {
        List<DashboardCaseDto> activeCases = new ArrayList<>();
        try {
            activeCases = patientServiceClient.getDoctorActiveCases(doctorId).getBody().getData().
                    stream().limit(3).map(this :: convertToDashboardCaseDto).toList();
            System.out.println("Doctor-Service: Recent active cases for doctor: "+
                    doctorId + " are: "+ activeCases.size());
        } catch (Exception e) {
            log.error("Failed to get recent cases for doctor {}: {}", doctorId, e.getMessage());
            e.printStackTrace();
        }
        return activeCases;
    }

    /**
     * Get upcoming appointments for dashboard
     */
    private List<DashboardAppointmentDto> getUpcomingAppointments(Long doctorId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endOfWeek = now.plusDays(7);

        return appointmentRepository
                .findByDoctorIdAndScheduledTimeBetween(doctorId, now, endOfWeek)
                .stream()
                .filter(appointment -> Arrays.asList(AppointmentStatus.SCHEDULED,
                                AppointmentStatus.CONFIRMED, AppointmentStatus.RESCHEDULED)
                        .contains(appointment.getStatus()))
                .sorted(Comparator.comparing(Appointment::getScheduledTime))
                .limit(3)
                .map(this::convertToDashboardAppointmentDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert CaseDto to DashboardCaseDto
     */
    private DashboardCaseDto convertToDashboardCaseDto(CaseDto caseDto) {
        String nextAction = determineNextAction(caseDto.getStatus().name());

        return DashboardCaseDto.builder()
                .id(caseDto.getId())
                //.patientName(maskPatientName(caseDto.getPatientName())) // Privacy consideration
                .caseTitle(caseDto.getCaseTitle())
                .status(caseDto.getStatus().name())
                .urgencyLevel(caseDto.getUrgencyLevel().name())
                .submittedAt(caseDto.getCreatedAt())
                .requiredSpecialization(caseDto.getRequiredSpecialization())
                .nextAction(nextAction)
                .build();
    }

    /**
     * Convert Appointment to DashboardAppointmentDto
     */
    private DashboardAppointmentDto convertToDashboardAppointmentDto(Appointment appointment) {
        // Get patient name from case (you might need to implement this)
        String patientName = getPatientNameForAppointment(appointment);

        return DashboardAppointmentDto.builder()
                .id(appointment.getId())
                .patientName(patientName)
                .scheduledTime(appointment.getScheduledTime())
                .consultationType(appointment.getConsultationType().toString())
                .status(appointment.getStatus().toString())
                .caseId(appointment.getCaseId())
                .urgencyLevel(getUrgencyLevelForAppointment(appointment.getCaseId()))
                .duration(appointment.getDuration())
                .build();
    }


    private Double calculateTotalEarnings(Long doctorId) {

        Double totalEarnings = 0.0;
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        List<PaymentHistoryDto> paymentHistoryDtoList = new ArrayList<>();
        try{
            paymentHistoryDtoList = paymentServiceClient.getDoctorPaymentHistory(doctor.getId()).getBody().getData();
        }catch(Exception e) {
            paymentHistoryDtoList = null;
            e.printStackTrace();
        }

        if( paymentHistoryDtoList != null && !paymentHistoryDtoList.isEmpty() ) {
            BigDecimal total = new BigDecimal("00.00");;
            for(PaymentHistoryDto paymentHistoryDto : paymentHistoryDtoList) {
                if( paymentHistoryDto.getPaymentType().equals(PaymentType.CONSULTATION.name())) {
                    if( paymentHistoryDto.getAmount() != null )
                        total = total.add(paymentHistoryDto.getAmount());
                }
            }
            totalEarnings = Double.valueOf( total.toString() );
        }
        return totalEarnings;
    }

    /**
     * Determine next action based on case status
     */
    private String determineNextAction(String status) {
        switch (status) {
            case "ASSIGNED":
                return "Accept or reject case";
            case "ACCEPTED":
                return "Schedule appointment";
            case "SCHEDULED":
                return "Wait for payment";
            case "IN_PROGRESS":
                return "Complete consultation";
            default:
                return "Review case";
        }
    }

    /**
     * Mask patient name for privacy
     */
    private String maskPatientName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "Anonymous Patient";
        }
        String[] parts = fullName.split(" ");
        if (parts.length >= 2) {
            return parts[0] + " " + parts[1].charAt(0) + ".";
        }
        return parts[0];
    }

    /**
     * Get patient name for appointment
     */
    private String getPatientNameForAppointment(Appointment appointment) {
        try {
            // You might need to call patient service to get patient name
            // For now, return a placeholder
            return "Patient #" + appointment.getPatientId();
        } catch (Exception e) {
            return "Unknown Patient";
        }
    }

    /**
     * Get urgency level for appointment's case
     */
    private String getUrgencyLevelForAppointment(Long caseId) {
        try {
            // Call patient service to get case details
            // For now, return a default value
            return "ROUTINE";
        } catch (Exception e) {
            return "ROUTINE";
        }
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

    @Transactional
    public void confirmAppointment(Long caseId, Long patientId, Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        Appointment appointment = appointmentRepository.findByCaseIdAndPatientIdAndDoctorId(caseId, patientId, doctorId)
                .orElseThrow(() -> new BusinessException("Appointment not found", HttpStatus.NOT_FOUND));

        if (!appointment.getDoctor().getId().equals(doctor.getId())) {
            throw new BusinessException("Unauthorized access", HttpStatus.FORBIDDEN);
        }
        appointment.setStatus(AppointmentStatus.CONFIRMED);
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

    /// // Helpers:


    /**
     * Update emergency mode settings
     */
    private void updateEmergencyMode(Doctor doctor, AvailabilityDto dto) {
        if (dto.getEmergencyMode() != null) {
            if (dto.getEmergencyMode() && !doctor.getEmergencyMode()) {
                // Enable emergency mode
                doctor.enableEmergencyMode(dto.getEmergencyReason() != null ?
                        dto.getEmergencyReason() : "Emergency mode enabled by doctor");
                log.info("Emergency mode enabled for doctor {}", doctor.getId());

            } else if (!dto.getEmergencyMode() && doctor.getEmergencyMode()) {
                // Disable emergency mode
                doctor.disableEmergencyMode();
                log.info("Emergency mode disabled for doctor {}", doctor.getId());
            }
        }
    }

    /**
     * Update available time slots with validation
     */
    private void updateAvailableTimeSlots(Doctor doctor, Set<TimeSlot> newTimeSlots) {
        try {
            // Validate time slots for overlaps
            validateTimeSlots(newTimeSlots);

            // Clear existing time slots
            doctor.getAvailableTimeSlots().clear();

            // Add new time slots
            if (newTimeSlots != null && !newTimeSlots.isEmpty()) {
                for (TimeSlot slot : newTimeSlots) {
                    // Ensure all time slots are marked as available by default
                    if (slot.getIsAvailable() == null) {
                        slot.setIsAvailable(true);
                    }
                    doctor.getAvailableTimeSlots().add(slot);
                }
                log.info("Updated {} time slots for doctor {}", newTimeSlots.size(), doctor.getId());
            } else {
                log.info("Cleared all time slots for doctor {} (24/7 availability)", doctor.getId());
            }

        } catch (Exception e) {
            log.error("Error updating time slots for doctor {}: {}", doctor.getId(), e.getMessage());
            throw new BusinessException("Invalid time slot configuration: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Validate time slots for overlaps and logical consistency
     */
    private void validateTimeSlots(Set<TimeSlot> timeSlots) {
        if (timeSlots == null || timeSlots.isEmpty()) {
            return;
        }

        for (TimeSlot slot1 : timeSlots) {
            // Validate individual slot
            if (slot1.getStartTime() == null || slot1.getEndTime() == null) {
                throw new IllegalArgumentException("Time slot must have both start and end time");
            }

            if (slot1.getStartTime().isAfter(slot1.getEndTime()) ||
                    slot1.getStartTime().equals(slot1.getEndTime())) {
                throw new IllegalArgumentException("Start time must be before end time");
            }

            if (slot1.getDayOfWeek() == null) {
                throw new IllegalArgumentException("Time slot must specify day of week");
            }

            // Check for overlaps with other slots
            for (TimeSlot slot2 : timeSlots) {
                if (slot1 != slot2 && slot1.overlaps(slot2)) {
                    throw new IllegalArgumentException(
                            String.format("Time slot overlap detected: %s %s-%s overlaps with %s-%s",
                                    slot1.getDayOfWeek(),
                                    slot1.getStartTime(),
                                    slot1.getEndTime(),
                                    slot2.getStartTime(),
                                    slot2.getEndTime()));
                }
            }
        }
    }

    /**
     * Update capacity settings
     */
    private void updateCapacitySettings(Doctor doctor, AvailabilityDto dto) {
        if (dto.getMaxActiveCases() != null && dto.getMaxActiveCases() > 0) {
            doctor.setMaxActiveCases(dto.getMaxActiveCases());
            log.info("Updated max active cases to {} for doctor {}",
                    dto.getMaxActiveCases(), doctor.getId());
        }

        if (dto.getMaxDailyAppointments() != null && dto.getMaxDailyAppointments() > 0) {
            doctor.setMaxDailyAppointments(dto.getMaxDailyAppointments());
            log.info("Updated max daily appointments to {} for doctor {}",
                    dto.getMaxDailyAppointments(), doctor.getId());
        }
    }

    /**
     * Update workload percentage based on availability changes
     */
    private void updateWorkloadBasedOnAvailability(Doctor doctor) {
        if (!doctor.getIsAvailable()) {
            // If doctor is not available, don't change workload but update timestamp
            doctor.setLastWorkloadUpdate(LocalDateTime.now());
        } else {
            // If doctor becomes available, you might want to trigger workload recalculation
            // This could be done via the workload service
            doctor.setLastWorkloadUpdate(LocalDateTime.now());

            // Optionally trigger workload service update (if available)
            // workloadService.loadDoctorWorkload(doctor.getId());
        }
    }

    /**
     * Log availability changes for audit trail
     */
    private void logAvailabilityChange(Doctor doctor, Boolean previousAvailability,
                                       Boolean previousEmergencyMode, AvailabilityDto dto) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("Availability change for doctor ").append(doctor.getId()).append(": ");

        if (!previousAvailability.equals(doctor.getIsAvailable())) {
            logMessage.append("availability ").append(previousAvailability).append(" -> ")
                    .append(doctor.getIsAvailable()).append("; ");
        }

        if (!previousEmergencyMode.equals(doctor.getEmergencyMode())) {
            logMessage.append("emergency mode ").append(previousEmergencyMode).append(" -> ")
                    .append(doctor.getEmergencyMode()).append("; ");
        }

        if (dto.getReason() != null) {
            logMessage.append("reason: ").append(dto.getReason());
        }

        log.info(logMessage.toString());
    }

    /**
     * Handle notifications for significant availability changes
     */
    private void handleAvailabilityNotifications(Doctor doctor, Boolean previousAvailability,
                                                 AvailabilityDto dto) {
        // Only send notifications for significant changes
        if (!previousAvailability.equals(doctor.getIsAvailable())) {
            try {
                String notificationMessage;
                String notificationTitle;

                if (doctor.getIsAvailable()) {
                    notificationTitle = "Doctor Available";
                    notificationMessage = String.format("Dr. %s is now available for consultations",
                            doctor.getFullName());
                } else {
                    notificationTitle = "Doctor Unavailable";
                    notificationMessage = String.format("Dr. %s is currently unavailable",
                            doctor.getFullName());
                    if (dto.getReason() != null) {
                        notificationMessage += ". Reason: " + dto.getReason();
                    }
                }

                // Send notification via event producer (if available)
                // doctorEventProducer.sendDoctorAvailabilityUpdate(doctor.getId(),
                //                                                 doctor.getIsAvailable(),
                //                                                 notificationMessage);

            } catch (Exception e) {
                log.warn("Failed to send availability notification for doctor {}: {}",
                        doctor.getId(), e.getMessage());
            }
        }
    }
}
