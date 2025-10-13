package com.doctorservice.service;

import com.commonlibrary.dto.*;
import com.commonlibrary.entity.*;
import com.commonlibrary.exception.BusinessException;
import com.doctorservice.dto.*;
import com.doctorservice.entity.*;
import com.doctorservice.feign.NotificationServiceClient;
import com.doctorservice.feign.PaymentServiceClient;
import com.doctorservice.kafka.DoctorEventProducer;
import com.doctorservice.repository.*;
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
import java.time.format.DateTimeFormatter;
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
    private final PdfGenerationService pdfGenerationService;
    private final AppointmentReminderService appointmentReminderService;
    private final DoctorSettingsRepository doctorSettingsRepository;

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
                .subSpecializations(dto.getSubSpecializations())
                .caseRate(dto.getCaseRate())
                .emergencyRate(dto.getEmergencyRate())
                .hourlyRate(dto.getHourlyRate())
                .verificationStatus(VerificationStatus.PENDING)
                .emergencyRate(dto.getEmergencyRate())
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
        if (dto.getPrimarySpecialization() != null && !dto.getPrimarySpecialization().trim().isEmpty()) {
            doctor.setPrimarySpecialization(dto.getPrimarySpecialization().trim());
        }

        if (dto.getSubSpecializations() != null && !dto.getSubSpecializations().isEmpty()) {
            doctor.setSubSpecializations(new HashSet<>(dto.getSubSpecializations()));
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
        if (dto.getCaseRate() != null && dto.getCaseRate() >= 0) {
            doctor.setCaseRate(dto.getCaseRate());
        }

        if (dto.getHourlyRate() != null && dto.getHourlyRate() >= 0) {
            doctor.setHourlyRate(dto.getHourlyRate());
        }

        if (dto.getEmergencyRate() != null && dto.getEmergencyRate() >= 0) {
            doctor.setEmergencyRate(dto.getEmergencyRate());
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

        //Validate case fees are set
        List<CaseDto> cases = patientServiceClient.getDoctorActiveCases(doctor.getId()).getBody().getData();
        if( cases != null && !cases.isEmpty()) {
            CaseDto medicalCase = cases.stream().filter(c->c.getId().equals(dto.getCaseId())).findFirst().
                    orElseThrow(() ->new BusinessException("Case not found for the provided appointment", HttpStatus.NOT_FOUND ));
            if(medicalCase.getConsultationFee() == null) {
                throw new BusinessException("Case fees not set yet, you cann't schedule an " +
                        " appointment for a case unless you set its fee", HttpStatus.CONFLICT);
            }
        }
        else{
            throw new BusinessException("Case not found for the provided appointment", HttpStatus.NOT_FOUND );
        }

        // Validate appointment time is in the future
        if (dto.getScheduledTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Cannot schedule appointment in the past", HttpStatus.BAD_REQUEST);
        }

        // Validate appointment conflicts
        validateAppointmentConflict(doctor.getId(), dto.getScheduledTime(), dto.getDuration(), null);

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

        //TODO
        // Here must be changed to Kafka instead of Feign

        // Update case status to SCHEDULED
        patientServiceClient.updateCaseStatus(dto.getCaseId(), "SCHEDULED", doctor.getId());

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
    public ConsultationReportDto createConsultationReport(Long userId, ConsultationReportDto dto) {

        //Some Validation:
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        Appointment appointment = appointmentRepository.findById(dto.getAppointmentId())
                .orElseThrow(() -> new BusinessException("Appointment not found", HttpStatus.NOT_FOUND));

        if (!appointment.getDoctor().getId().equals(doctor.getId())) {
            throw new BusinessException("Unauthorized access to appointment", HttpStatus.FORBIDDEN);
        }
        System.out.println("Creating Medical Report ============> Checking doctor and appointment validation - done");

        List<CaseDto> cases = patientServiceClient.getDoctorCompletedCases(doctor.getId()).getBody().getData();
        if( cases != null && !cases.isEmpty()) {
            cases.stream().filter(c->c.getId().equals(dto.getCaseId())).findFirst().orElseThrow(()->
             new BusinessException("Cannot create report. Case not found",
                    HttpStatus.NOT_FOUND));
        }
        System.out.println("Creating Medical Report ============> Checking case existance - done");

        consultationReportRepository.findByCaseId(dto.getCaseId())
                .stream()
                .findFirst()
                .ifPresent(existing -> {
                    throw new BusinessException(
                            "A report already exists for this case. Use update instead.",
                            HttpStatus.CONFLICT
                    );
                });
        System.out.println("Creating Medical Report ============> Checking report already existed - done");

        // Creating Medical Report
        ConsultationReport report = ConsultationReport.builder()
                .appointment(appointment)
                .doctor(doctor)
                .caseId(dto.getCaseId())
                .patientId(appointment.getPatientId())
                .diagnosis(dto.getDiagnosis())
                .recommendations(dto.getRecommendations())
                .prescriptions(dto.getPrescriptions())
                .followUpInstructions(dto.getFollowUpInstructions())
                .requiresFollowUp(dto.getRequiresFollowUp())
                .nextAppointmentSuggested(dto.getNextAppointmentSuggested())
                .doctorNotes(dto.getDoctorNotes())
                .status(ReportStatus.DRAFT)
                .doctorName(dto.getDoctorName())
                .patientName(dto.getPatientName())
                .build();

        // Update appointment status
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);
        System.out.println("Creating Medical Report ============> updating appointment to COMPLETED");

        // No need to Update case status to closed that the report still in DRAFT state
//        patientServiceClient.updateCaseStatus(dto.getCaseId(), "CLOSED", doctor.getId());
//
//        System.out.println("Creating Medical Report ============> updating case status to CLOSED");

        /*TODO
        *  Update doctor's consultation count*/


        //doctor.setConsultationCount(doctor.getConsultationCount() + 1);
        consultationReportRepository.save(report);
        System.out.println("Creating Medical Report ============> saving the medical report as a DRAFT");

        ConsultationReportDto reportDto = convertToReportDto(report);
        return reportDto;
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
        ModelMapper modelMapper = new ModelMapper();

        dto = modelMapper.map(doctor, DoctorProfileDto.class);

//        dto.setId(doctor.getId());
//        dto.setUserId(doctor.getUserId());
//        dto.setFullName(doctor.getFullName());
//        dto.setLicenseNumber(doctor.getLicenseNumber());
//        dto.setPrimarySpecializationCode(doctor.getPrimarySpecialization());
//
//        // Convert sets to comma-separated strings for frontend compatibility
//        if (doctor.getSubSpecializations() != null && !doctor.getSubSpecializations().isEmpty()) {
//            dto.setSubSpecializationCodes(doctor.getSubSpecializations());
//        }
//
//        if (doctor.getQualifications() != null && !doctor.getQualifications().isEmpty()) {
//            dto.setQualifications(String.join(", ", doctor.getQualifications()));
//        }
//
//        if (doctor.getLanguages() != null && !doctor.getLanguages().isEmpty()) {
//            dto.setLanguages(String.join(", ", doctor.getLanguages()));
//        }
//
//        dto.setVerificationStatus(doctor.getVerificationStatus());
//        dto.setYearsOfExperience(doctor.getYearsOfExperience());
//        dto.setPhoneNumber(doctor.getPhoneNumber());
//        dto.setEmail(doctor.getEmail());
//        dto.setHospitalAffiliation(doctor.getHospitalAffiliation());
//
//        // Convert pricing fields
//        if (doctor.getCaseRate() != null) {
//            dto.setBaseConsultationFee(doctor.getCaseRate());
//            dto.setCaseRate(doctor.getCaseRate());
//        }
//
//        if (doctor.getHourlyRate() != null) {
//            dto.setHourlyRate(doctor.getHourlyRate());
//        }
//
//        if (doctor.getEmergencyRate() != null) {
//            dto.setUrgentCaseFee(doctor.getEmergencyRate());
//            dto.setEmergencyRate(doctor.getEmergencyRate());
//        }
//
//        // Capacity settings
//        if (doctor.getMaxActiveCases() != null) {
//            dto.setMaxConcurrentCases(doctor.getMaxActiveCases());
//        }


        return dto;
    }

    // 10. Get Profile Implementation
    public DoctorProfileDto getProfile(Long userId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));
        return mapToDto(doctor);
    }

    public CaseDataForMedicalReportDto getCaseDetailsForMedicalReport(Long userId, Long caseId) {
        CaseDataForMedicalReportDto customCaseData = new CaseDataForMedicalReportDto();

        //Get Doctor:
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("getCaseDetailsForMedicalReport ===> Doctor not found",
                        HttpStatus.NOT_FOUND));

        //Get appointment info:
        Appointment appointment = appointmentRepository.findByCaseIdAndDoctorIdAndStatus(caseId, doctor.getId(),
                AppointmentStatus.COMPLETED).
                orElseThrow(() -> new BusinessException("getCaseDetailsForMedicalReport ===> Appointment not found",
                        HttpStatus.NOT_FOUND));

        //Get Case Details:
        List<CaseDto> cases = patientServiceClient.getAllDoctorCases(doctor.getId()).getBody().getData();
        if( cases == null || cases.isEmpty() )
            throw new BusinessException("getCaseDetailsForMedicalReport ===> No Cases found",
                    HttpStatus.NOT_FOUND);

        CaseDto medicalCase = cases.stream().filter(c -> c.getId().equals(caseId)).
                findFirst().get();
        if( medicalCase == null )
            throw new BusinessException("getCaseDetailsForMedicalReport ===> Case " + caseId +  " not found",
                    HttpStatus.NOT_FOUND);
        //Get Patient info:
        CustomPatientDto patientDto = patientServiceClient.getCustomPatientInfo(caseId, doctor.getId()).
                getBody().getData();

        //Some validation
        if(!Objects.equals(appointment.getDoctor().getId(), doctor.getId()) ||
                ( patientDto !=null && !Objects.equals(appointment.getPatientId(), patientDto.getId())  )){
            throw new BusinessException("getCaseDetailsForMedicalReport ===> Doctor, Patient and Appointment conflict - No matching",
                    HttpStatus.UNAUTHORIZED);
        }

        customCaseData.setCaseId(caseId);
        customCaseData.setDoctorId(doctor.getId());
        customCaseData.setAppointmentId(appointment.getId());
        customCaseData.setPatientId(patientDto.getId());
        customCaseData.setDoctorName(doctor.getFullName());
        customCaseData.setPatientName(patientDto.getFullName());
        customCaseData.setCaseTitle(medicalCase.getCaseTitle());
        customCaseData.setCaseSubmittedAt(medicalCase.getSubmittedAt());
        customCaseData.setCaseRequiredSpecialization(medicalCase.getRequiredSpecialization());
        customCaseData.setCaseDescription(medicalCase.getDescription());


        return customCaseData;
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

    public List<CaseDto> getAllCases(Long userId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        List<CaseDto> allDoctorCases = new ArrayList<>();
        try{
            allDoctorCases = patientServiceClient.getAllDoctorCases(doctor.getId()).getBody().getData();
            System.out.println("Doctor-Service: All Doctor cases, for doctor: "+
                    doctor.getId() + " are: "+ allDoctorCases.size());
        }catch (Exception e){
            log.error("Failed to get Active cases for doctor: " + doctor.getFullName() +
                    " - Id:" + doctor.getId(), e);
            e.printStackTrace();
        }
        return allDoctorCases;
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
    public Appointment rescheduleAppointment(Long userId, Long appointmentId, RescheduleAppointmentDto dto) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException("Appointment not found", HttpStatus.NOT_FOUND));

        // Verify the appointment belongs to this doctor
        if (!appointment.getDoctor().getId().equals(doctor.getId())) {
            throw new BusinessException("Unauthorized to reschedule this appointment", HttpStatus.FORBIDDEN);
        }

        // Validate appointment can be rescheduled
        if (appointment.getStatus() == AppointmentStatus.COMPLETED ||
                appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Cannot reschedule a " + appointment.getStatus() + " appointment",
                    HttpStatus.BAD_REQUEST);
        }

        // Validate new appointment time is in the future
        if (dto.getScheduledTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Cannot reschedule appointment to a past time", HttpStatus.BAD_REQUEST);
        }

        // Validate the new time doesn't conflict with other appointments
        // Exclude the current appointment from conflict check
        validateAppointmentConflict(doctor.getId(), dto.getScheduledTime(),
                appointment.getDuration(), appointmentId);

        // Update appointment
        appointment.setScheduledTime(dto.getScheduledTime());
        appointment.setRescheduleReason(dto.getReason());
        appointment.setRescheduleCount(appointment.getRescheduleCount() + 1);
        appointment.setStatus(AppointmentStatus.RESCHEDULED);

        Appointment updated = appointmentRepository.save(appointment);

        appointmentReminderService.cancelRemindersForAppointment(appointmentId);

        //Todo fix this
//        // Send notification to patient
//        doctorEventProducer.SendAppointmentRescheduleNotification(
//                appointment.getPatientId(),
//                appointmentId,
//                dto.getScheduledTime(),
//                dto.getReason(),
//                doctor.getFullName()
//        );

        return updated;
    }

    @Transactional
    public void completeAppointment(Long userId, CompleteAppointmentDto dto) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        Appointment appointment = appointmentRepository.findById(dto.getAppointmentId())
                .orElseThrow(() -> new BusinessException("Appointment not found", HttpStatus.NOT_FOUND));

        if (!appointment.getDoctor().getId().equals(doctor.getId())) {
            throw new BusinessException("Unauthorized access", HttpStatus.FORBIDDEN);
        }
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);

        System.out.println("completeAppointment ====>  Doctor:Id: " + doctor.getId() +
                ", Appointment Id: "+ appointment.getId() +
                ", Patient Id: " + appointment.getPatientId() +
                ", Case Id: " + dto.getCaseId());

        //Update Case:
        List<CaseDto> cases = patientServiceClient.getDoctorActiveCases(doctor.getId()).getBody().getData();
        if( cases != null && !cases.isEmpty() ) {
            System.out.println("completeAppointment ====>  cases for doctor: " +cases.size());
            CaseDto caseDto = cases.stream().filter(c->c.getId().equals(dto.getCaseId())).findFirst().
                    orElseThrow(()-> new BusinessException("completeAppointment ====>  Failed to update case "+
                            dto.getCaseId() +
                            " status to CONSULTATION_COMPLETE ", HttpStatus.NOT_FOUND));

            System.out.println("Send kafka event to update case "+ caseDto.getId() +
                    " status to: CONSULTATION_COMPLETE");

            //Send update Case Kafka event to update case status to CONSULTATION_COMPLETE
            doctorEventProducer.sendCaseStatusUpdateEventFromDoctor(caseDto.getId(), CaseStatus.IN_PROGRESS.name(),
                    CaseStatus.CONSULTATION_COMPLETE.name(), dto.getPatientId(), doctor.getId());
        }
        else{
           throw new BusinessException("completeAppointment ====> Failed to update case "+ dto.getCaseId() +
                   " status to CONSULTATION_COMPLETE ", HttpStatus.NOT_FOUND);
        }
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

        // CREATE REMINDERS for both doctor and patient
        appointmentReminderService.createRemindersForAppointment(appointment.getId());
        log.info("Appointment {} confirmed and reminders created", appointment.getId());
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

        appointmentReminderService.cancelRemindersForAppointment(appointmentId);

        //Send Kafka notification for the Patient, and event for the admin:
        doctorEventProducer.sendAppointmentCancellationEvent( appointmentId, appointment.getCaseId(),
                appointment.getDoctor().getId(), appointment.getPatientId(),
                appointment.getScheduledTime(), reason);
    }

    public List<ConsultationReportDto> getConsultationReports(Long userId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        List<ConsultationReport> reports = consultationReportRepository.findByDoctorId(doctor.getId());
        List<ConsultationReportDto> reportDtos = reports.stream().map(this::convertToReportDto).toList();

        return reportDtos;
    }

    public ConsultationReportDto convertToReportDto(ConsultationReport report) {
        ConsultationReportDto reportDto = new ConsultationReportDto();
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.map(report, reportDto);
        return reportDto;
    }

    @Transactional
    public String exportReportToPdf(Long userId, Long reportId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        ConsultationReport report = consultationReportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException("Report not found", HttpStatus.NOT_FOUND));

        if (!report.getDoctor().getId().equals(doctor.getId())) {
            throw new BusinessException("Unauthorized", HttpStatus.FORBIDDEN);
        }

        // Check if already exported
        if (report.getStatus() == ReportStatus.FINALIZED && report.getPdfFileLink() != null) {
            log.warn("Report {} already exported. Returning existing PDF URL", reportId);
            return report.getPdfFileLink();
        }

        // Validate required fields before export
        validateReportForExport(report);

        try{
            // Generate PDF
            String pdfUrl = pdfGenerationService.generateReportPdf(report);

            // Update report
            report.setPdfFileLink(pdfUrl);
            report.setStatus(ReportStatus.FINALIZED);
            report.setExportedAt(LocalDateTime.now());
            report.setFinalizedAt(LocalDateTime.now());
            consultationReportRepository.save(report);

            // Send Kafka event to patient-service
            doctorEventProducer.sendReportExportedEvent(report.getCaseId(), pdfUrl,
                    doctor.getId(), report.getPatientId(), report.getId());

            return pdfUrl;

        } catch (Exception ex) {
            log.error("Failed to export report {} to PDF", reportId, ex);
            throw new BusinessException(
                    "Failed to export report to PDF: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    public ConsultationReport getConsultationReportsById(Long userId, Long consultationReportId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        ConsultationReport report = consultationReportRepository.findById(consultationReportId).orElseThrow(() ->
                new BusinessException("Report not found", HttpStatus.NOT_FOUND));

        if( doctor.getId().equals(report.getDoctor().getId()) )     {
            throw new BusinessException("Report doesn't belong to the provided doctor", HttpStatus.UNAUTHORIZED);
        }

        if( !report.getStatus().equals(ReportStatus.FINALIZED) || report.getPdfFileLink()==null ) {
            throw new BusinessException("Please check eport status or link ", HttpStatus.CONFLICT);
        }

        return report;
    }

    /**
     * Update consultation report (only if DRAFT)
     */
    @Transactional
    public ConsultationReportDto updateConsultationReport(Long userId, Long reportId, UpdateReportDto dto) {
        log.info("Updating consultation report {} for user {}", reportId, userId);

        // Validate doctor
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        // Get report
        ConsultationReport report = consultationReportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException("Report not found", HttpStatus.NOT_FOUND));

        // Check authorization
        if (!report.getDoctor().getId().equals(doctor.getId())) {
            throw new BusinessException("Unauthorized access", HttpStatus.FORBIDDEN);
        }

        // Check if report is still in DRAFT
        if (report.getStatus() == ReportStatus.FINALIZED) {
            throw new BusinessException(
                    "Cannot update finalized report. Report has been exported to PDF.",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Update fields
        if (dto.getDiagnosis() != null) {
            report.setDiagnosis(dto.getDiagnosis());
        }
        if (dto.getRecommendations() != null) {
            report.setRecommendations(dto.getRecommendations());
        }
        if (dto.getPrescriptions() != null) {
            report.setPrescriptions(dto.getPrescriptions());
        }
        if (dto.getFollowUpInstructions() != null) {
            report.setFollowUpInstructions(dto.getFollowUpInstructions());
        }
        if (dto.getDoctorNotes() != null) {
            // Append to existing notes with timestamp
            String timestamp = LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String existingNotes = report.getDoctorNotes() != null ? report.getDoctorNotes() : "";
            report.setDoctorNotes(existingNotes + "\n\n[" + timestamp + "] " + dto.getDoctorNotes());
        }
//        if (dto.getRequiresFollowUp() != null) {
//            report.setRequiresFollowUp(dto.getRequiresFollowUp());
//        }
//        if (dto.getNextAppointmentSuggested() != null) {
//            report.setNextAppointmentSuggested(dto.getNextAppointmentSuggested());
//        }

        report.setUpdatedAt(LocalDateTime.now());
        ConsultationReport updatedReport = consultationReportRepository.save(report);

        log.info("Consultation report {} updated successfully", reportId);
        ConsultationReportDto  reportDto = convertToReportDto(updatedReport);
        return reportDto;
    }

    /**
     * Get all consultation reports for doctor (with optional status filter)
     */
    public List<ConsultationReportDto> getConsultationReportsByStatus(Long userId, ReportStatus status) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        List<ConsultationReport> reports = consultationReportRepository.findByDoctorId(doctor.getId());
        List<ConsultationReportDto> reportDtos = reports.stream().map(this::convertToReportDto).toList();

        if (status == null) {
            reports = consultationReportRepository.findByDoctorId(doctor.getId());
            reportDtos = reports.stream().map(this::convertToReportDto).toList();
        }

        return reportDtos;
    }

    /**
     * Get single consultation report by ID
     */
    public ConsultationReportDto getConsultationReportById(Long userId, Long reportId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        ConsultationReport report = consultationReportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException("Report not found", HttpStatus.NOT_FOUND));

        if (!report.getDoctor().getId().equals(doctor.getId())) {
            throw new BusinessException("Unauthorized access", HttpStatus.FORBIDDEN);
        }
        ConsultationReportDto reportDto = convertToReportDto(report);
        return reportDto;
    }

    /**
     * Delete consultation report (only DRAFT reports can be deleted)
     */
    @Transactional
    public void deleteConsultationReport(Long userId, Long reportId) {
        log.info("Deleting consultation report {} for user {}", reportId, userId);

        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        ConsultationReport report = consultationReportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException("Report not found", HttpStatus.NOT_FOUND));

        if (!report.getDoctor().getId().equals(doctor.getId())) {
            throw new BusinessException("Unauthorized access", HttpStatus.FORBIDDEN);
        }

        if (report.getStatus() == ReportStatus.FINALIZED) {
            throw new BusinessException(
                    "Cannot delete finalized report. Finalized reports are permanent.",
                    HttpStatus.BAD_REQUEST
            );
        }

        consultationReportRepository.delete(report);
        log.info("Consultation report {} deleted successfully", reportId);
    }

    /**
     * Validate that report has all required fields before export
     */
    private void validateReportForExport(ConsultationReport report) {
        StringBuilder errors = new StringBuilder();

        if (report.getDiagnosis() == null || report.getDiagnosis().trim().isEmpty()) {
            errors.append("Diagnosis is required. ");
        }
        if (report.getRecommendations() == null || report.getRecommendations().trim().isEmpty()) {
            errors.append("Recommendations are required. ");
        }
        if (report.getPrescriptions() == null || report.getPrescriptions().trim().isEmpty()) {
            errors.append("Prescriptions are required (enter 'None' if not applicable). ");
        }
        if (report.getRequiresFollowUp() == null) {
            errors.append("Follow-up requirement must be specified. ");
        }

        if (errors.length() > 0) {
            throw new BusinessException(
                    "Cannot export incomplete report. Missing: " + errors.toString().trim(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    // DTO for case status validation
    @lombok.Data
    private static class CaseStatusResponse {
        private Long caseId;
        private CaseStatus status;
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


    public List<NotificationDto> getMyNotificationsByUserId(Long userId){
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));
        List<NotificationDto> dtos = new ArrayList<>();
        try{
            dtos = notificationServiceClient.getUserNotifications(doctor.getId()).getBody().getData();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return dtos;
    }


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


    /**
     * Check if a specific time slot is available for the doctor
     *
     * @param doctorId The doctor's ID
     * @param scheduledTime The proposed appointment time
     * @param duration The appointment duration
     * @param excludeAppointmentId Optional appointment ID to exclude (for rescheduling)
     * @return true if the slot is available, false otherwise
     */
    public boolean isTimeSlotAvailable(Long doctorId, LocalDateTime scheduledTime,
                                       Integer duration, Long excludeAppointmentId) {
        try {
            validateAppointmentConflict(doctorId, scheduledTime, duration, excludeAppointmentId);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    /**
     * Get detailed availability information including conflicting appointments
     *
     * @param doctorId The doctor's ID
     * @param scheduledTime The proposed appointment time
     * @param duration The appointment duration
     * @param excludeAppointmentId Optional appointment ID to exclude
     * @return Detailed availability information
     */
    public SlotAvailabilityDto getSlotAvailabilityDetails(Long doctorId, LocalDateTime scheduledTime,
                                                          Integer duration, Long excludeAppointmentId) {
        LocalDateTime appointmentEndTime = scheduledTime.plusMinutes(duration != null ? duration : 30);
        int bufferMinutes = 15;

        List<Appointment> conflicts = appointmentRepository.findOverlappingAppointments(
                doctorId,
                scheduledTime.minusMinutes(bufferMinutes),
                appointmentEndTime.plusMinutes(bufferMinutes),
                excludeAppointmentId
        );

        boolean isAvailable = conflicts.isEmpty();
        LocalDateTime conflictTime = conflicts.isEmpty() ? null : conflicts.get(0).getScheduledTime();

        return SlotAvailabilityDto.builder()
                .scheduledTime(scheduledTime)
                .duration(duration)
                .available(isAvailable)
                .message(isAvailable ?
                        "Time slot is available" :
                        "Time slot conflicts with existing appointment")
                .conflictingAppointmentTime(conflictTime)
                .build();
    }

    /**
     * Validates that the new appointment doesn't conflict with existing appointments
     *
     * @param doctorId The doctor's ID
     * @param scheduledTime The proposed appointment time
     * @param duration The appointment duration in minutes
     * @param excludeAppointmentId Optional appointment ID to exclude (for rescheduling)
     * @throws BusinessException if there's a scheduling conflict
     */
    private void validateAppointmentConflict(Long doctorId, LocalDateTime scheduledTime,
                                             Integer duration, Long excludeAppointmentId) {
        // Calculate end time of the new appointment
        int appointmentDuration = duration != null ? duration : 30;
        LocalDateTime appointmentEndTime = scheduledTime.plusMinutes(appointmentDuration);

        // Define buffer time (e.g., 15 minutes between appointments)
        int bufferMinutes = 15;

        // Expand search range to include buffer time
        LocalDateTime searchStartTime = scheduledTime.minusMinutes(appointmentDuration + bufferMinutes);
        LocalDateTime searchEndTime = appointmentEndTime.plusMinutes(appointmentDuration + bufferMinutes);

        // Get all potentially conflicting appointments within the expanded time range
        List<Appointment> potentialConflicts = appointmentRepository
                .findByDoctorIdAndScheduledTimeBetweenAndStatusNot(
                        doctorId,
                        searchStartTime,
                        searchEndTime,
                        AppointmentStatus.CANCELLED
                );

        // Filter out NO_SHOW appointments and the appointment being rescheduled
        List<Appointment> existingAppointments = potentialConflicts.stream()
                .filter(apt -> apt.getStatus() != AppointmentStatus.NO_SHOW)
                .filter(apt -> excludeAppointmentId == null || !apt.getId().equals(excludeAppointmentId))
                .collect(Collectors.toList());

        // Check for conflicts with precise overlap detection
        for (Appointment existing : existingAppointments) {
            int existingDuration = existing.getDuration() != null ? existing.getDuration() : 30;
            LocalDateTime existingEndTime = existing.getScheduledTime().plusMinutes(existingDuration);

            // Check if appointments overlap (with buffer)
            boolean hasConflict = checkTimeOverlap(
                    scheduledTime.minusMinutes(bufferMinutes),
                    appointmentEndTime.plusMinutes(bufferMinutes),
                    existing.getScheduledTime(),
                    existingEndTime
            );

            if (hasConflict) {
                String conflictMessage = String.format(
                        "Appointment time conflicts with an existing appointment at %s (Duration: %d minutes). " +
                                "Please choose a different time slot.",
                        existing.getScheduledTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        existingDuration
                );
                throw new BusinessException(conflictMessage, HttpStatus.CONFLICT);
            }
        }
    }

    /**
     * Checks if two time ranges overlap
     *
     * @param start1 Start time of first appointment
     * @param end1 End time of first appointment
     * @param start2 Start time of second appointment
     * @param end2 End time of second appointment
     * @return true if the time ranges overlap
     */
    private boolean checkTimeOverlap(LocalDateTime start1, LocalDateTime end1,
                                     LocalDateTime start2, LocalDateTime end2) {
        // Two time ranges overlap if:
        // start1 < end2 AND start2 < end1
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    /**
     * Get available time slots for a doctor on a specific date
     * This method can be used by the frontend to show available slots
     *
     * @param doctorId The doctor's ID
     * @param date The date to check
     * @param duration The desired appointment duration
     * @return List of available time slots
     */
    public List<LocalDateTime> getAvailableTimeSlots(Long doctorId, LocalDate date, Integer duration) {
        List<LocalDateTime> availableSlots = new ArrayList<>();

        // Define working hours (e.g., 9 AM to 5 PM)
        LocalDateTime startTime = date.atTime(9, 0);
        LocalDateTime endTime = date.atTime(17, 0);

        // Get all appointments for this doctor on this date
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(23, 59, 59);

        List<Appointment> dayAppointments = appointmentRepository
                .findByDoctorIdAndScheduledTimeBetweenAndStatusNot(
                        doctorId,
                        dayStart,
                        dayEnd,
                        AppointmentStatus.CANCELLED
                );

        // Generate time slots (e.g., every 30 minutes)
        LocalDateTime currentSlot = startTime;
        int slotInterval = 30; // minutes
        int appointmentDuration = duration != null ? duration : 30;
        int bufferMinutes = 15;

        while (currentSlot.plusMinutes(appointmentDuration).isBefore(endTime)
                || currentSlot.plusMinutes(appointmentDuration).equals(endTime)) {

            LocalDateTime slotEndTime = currentSlot.plusMinutes(appointmentDuration);
            boolean isAvailable = true;

            // Check if this slot conflicts with any existing appointment
            for (Appointment appointment : dayAppointments) {
                LocalDateTime aptEndTime = appointment.getScheduledTime()
                        .plusMinutes(appointment.getDuration() != null ? appointment.getDuration() : 30);

                // Check overlap with buffer
                boolean hasConflict = checkTimeOverlap(
                        currentSlot.minusMinutes(bufferMinutes),
                        slotEndTime.plusMinutes(bufferMinutes),
                        appointment.getScheduledTime(),
                        aptEndTime
                );

                if (hasConflict) {
                    isAvailable = false;
                    break;
                }
            }

            if (isAvailable && currentSlot.isAfter(LocalDateTime.now())) {
                availableSlots.add(currentSlot);
            }

            currentSlot = currentSlot.plusMinutes(slotInterval);
        }

        return availableSlots;
    }

    /**
     * Get doctor payment history
     */
    public List<PaymentHistoryDto> getPaymentHistory(Long doctorId, Map<String, String> filters) {
        // If period filter is specified, use period-based query
        if (filters.containsKey("period")) {
            String period = filters.get("period");
            return getPaymentHistoryByPeriod(doctorId, period);
        }

        // If status filter is specified
        if (filters.containsKey("status")) {
            String status = filters.get("status");
            return paymentServiceClient.getDoctorPaymentHistoryByStatus(doctorId, status)
                    .getBody()
                    .getData();
        }

        // Default: get all payment history
        return paymentServiceClient.getDoctorPaymentHistory(doctorId)
                .getBody()
                .getData();
    }

    /**
     * Get payment history filtered by period
     */
    private List<PaymentHistoryDto> getPaymentHistoryByPeriod(Long doctorId, String period) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;

        switch (period.toLowerCase()) {
            case "week":
                startDate = now.minusWeeks(1);
                break;
            case "month":
                startDate = now.minusMonths(1);
                break;
            case "year":
                startDate = now.minusYears(1);
                break;
            case "all":
            default:
                // For "all", just get all history
                return paymentServiceClient.getDoctorPaymentHistory(doctorId)
                        .getBody()
                        .getData();
        }

        return paymentServiceClient.getDoctorPaymentHistoryByPeriod(
                doctorId,
                startDate.toString(),
                now.toString()
        ).getBody().getData();
    }

    /**
     * Get doctor earnings summary
     */
    public DoctorEarningsSummaryDto getEarningsSummary(Long doctorId, String period) {
        return paymentServiceClient.getDoctorEarningsSummary(doctorId, period)
                .getBody()
                .getData();
    }

    /**
     * Get doctor earnings statistics
     */
    public Map<String, Object> getEarningsStats(Long doctorId) {
        return paymentServiceClient.getDoctorEarningsStats(doctorId)
                .getBody()
                .getData();
    }

    /**
     * Get doctor settings
     */
    public DoctorSettingsDto getSettings(Long userId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        DoctorSettings settings = doctorSettingsRepository.findByDoctorId(doctor.getId())
                .orElseGet(() -> createDefaultSettings(doctor.getId()));

        return mapSettingsToDto(settings);
    }

    /**
     * Update doctor settings
     */
    @Transactional
    public DoctorSettingsDto updateSettings(Long userId, DoctorSettingsDto dto) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Doctor not found", HttpStatus.NOT_FOUND));

        DoctorSettings settings = doctorSettingsRepository.findByDoctorId(doctor.getId())
                .orElseGet(() -> createDefaultSettings(doctor.getId()));

        // Update notification preferences
        if (dto.getNotifications() != null) {
            DoctorSettingsDto.NotificationPreferences notif = dto.getNotifications();
            if (notif.getEmail() != null) settings.setEmailNotifications(notif.getEmail());
            if (notif.getSms() != null) settings.setSmsNotifications(notif.getSms());
            if (notif.getPush() != null) settings.setPushNotifications(notif.getPush());
            if (notif.getNewCaseAssignment() != null)
                settings.setNewCaseAssignmentNotification(notif.getNewCaseAssignment());
            if (notif.getAppointmentReminders() != null)
                settings.setAppointmentRemindersNotification(notif.getAppointmentReminders());
            if (notif.getPatientMessages() != null)
                settings.setPatientMessagesNotification(notif.getPatientMessages());
            if (notif.getSystemUpdates() != null)
                settings.setSystemUpdatesNotification(notif.getSystemUpdates());
            if (notif.getPromotions() != null)
                settings.setPromotionsNotification(notif.getPromotions());
        }

        // Update availability preferences
        if (dto.getAvailability() != null) {
            DoctorSettingsDto.AvailabilityPreferences avail = dto.getAvailability();
            if (avail.getAutoAcceptCases() != null) settings.setAutoAcceptCases(avail.getAutoAcceptCases());
            if (avail.getMaxDailyCases() != null) settings.setMaxDailyCases(avail.getMaxDailyCases());
            if (avail.getAllowEmergencyCases() != null)
                settings.setAllowEmergencyCases(avail.getAllowEmergencyCases());
            if (avail.getRequiresConsultationFee() != null)
                settings.setRequiresConsultationFee(avail.getRequiresConsultationFee());
        }

        // Update privacy preferences
        if (dto.getPrivacy() != null) {
            DoctorSettingsDto.PrivacyPreferences privacy = dto.getPrivacy();
            if (privacy.getProfileVisibility() != null)
                settings.setProfileVisibility(privacy.getProfileVisibility());
            if (privacy.getShowRating() != null) settings.setShowRating(privacy.getShowRating());
            if (privacy.getShowExperience() != null) settings.setShowExperience(privacy.getShowExperience());
            if (privacy.getAllowReviews() != null) settings.setAllowReviews(privacy.getAllowReviews());
        }

        // Update accessibility preferences
        if (dto.getAccessibility() != null) {
            DoctorSettingsDto.AccessibilityPreferences access = dto.getAccessibility();
            if (access.getTheme() != null) settings.setTheme(access.getTheme());
            if (access.getFontSize() != null) settings.setFontSize(access.getFontSize());
            if (access.getLanguage() != null) settings.setLanguage(access.getLanguage());
            if (access.getTimezone() != null) settings.setTimezone(access.getTimezone());
        }

        DoctorSettings savedSettings = doctorSettingsRepository.save(settings);
        return mapSettingsToDto(savedSettings);
    }

    /**
     * Create default settings for a doctor
     */
    private DoctorSettings createDefaultSettings(Long doctorId) {
        DoctorSettings settings = DoctorSettings.builder()
                .doctorId(doctorId)
                .emailNotifications(true)
                .smsNotifications(true)
                .pushNotifications(true)
                .newCaseAssignmentNotification(true)
                .appointmentRemindersNotification(true)
                .patientMessagesNotification(true)
                .systemUpdatesNotification(true)
                .promotionsNotification(false)
                .autoAcceptCases(false)
                .maxDailyCases(10)
                .allowEmergencyCases(true)
                .requiresConsultationFee(true)
                .profileVisibility("verified_patients")
                .showRating(true)
                .showExperience(true)
                .allowReviews(true)
                .theme("light")
                .fontSize("medium")
                .language("en")
                .timezone("UTC")
                .build();

        return doctorSettingsRepository.save(settings);
    }

    /**
     * Map DoctorSettings entity to DTO
     */
    private DoctorSettingsDto mapSettingsToDto(DoctorSettings settings) {
        return DoctorSettingsDto.builder()
                .notifications(DoctorSettingsDto.NotificationPreferences.builder()
                        .email(settings.getEmailNotifications())
                        .sms(settings.getSmsNotifications())
                        .push(settings.getPushNotifications())
                        .newCaseAssignment(settings.getNewCaseAssignmentNotification())
                        .appointmentReminders(settings.getAppointmentRemindersNotification())
                        .patientMessages(settings.getPatientMessagesNotification())
                        .systemUpdates(settings.getSystemUpdatesNotification())
                        .promotions(settings.getPromotionsNotification())
                        .build())
                .availability(DoctorSettingsDto.AvailabilityPreferences.builder()
                        .autoAcceptCases(settings.getAutoAcceptCases())
                        .maxDailyCases(settings.getMaxDailyCases())
                        .allowEmergencyCases(settings.getAllowEmergencyCases())
                        .requiresConsultationFee(settings.getRequiresConsultationFee())
                        .build())
                .privacy(DoctorSettingsDto.PrivacyPreferences.builder()
                        .profileVisibility(settings.getProfileVisibility())
                        .showRating(settings.getShowRating())
                        .showExperience(settings.getShowExperience())
                        .allowReviews(settings.getAllowReviews())
                        .build())
                .accessibility(DoctorSettingsDto.AccessibilityPreferences.builder()
                        .theme(settings.getTheme())
                        .fontSize(settings.getFontSize())
                        .language(settings.getLanguage())
                        .timezone(settings.getTimezone())
                        .build())
                .build();
    }

}
