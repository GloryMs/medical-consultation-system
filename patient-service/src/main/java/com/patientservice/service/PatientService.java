package com.patientservice.service;

import com.commonlibrary.dto.*;
import com.commonlibrary.entity.*;
import com.commonlibrary.exception.BusinessException;
import com.patientservice.dto.*;
import com.patientservice.entity.*;
import com.patientservice.feign.DoctorServiceClient;
import com.patientservice.feign.PaymentServiceClient;
import com.patientservice.feign.NotificationServiceClient;
import com.patientservice.kafka.PatientEventProducer;
import com.patientservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.commonlibrary.entity.CaseStatus.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientService {

    private final PatientRepository patientRepository;
    private final CaseRepository caseRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final DocumentRepository documentRepository;
    private final RescheduleRequestRepository rescheduleRequestRepository;
    private final PaymentServiceClient paymentServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final CaseAssignmentRepository caseAssignmentRepository;
    private final MedicalConfigurationService configService;
    private final SmartCaseAssignmentService assignmentService;
    private final CaseAssignmentRepository assignmentRepository;
    private final DoctorServiceClient doctorServiceClient;
    private final PatientEventProducer patientEventProducer;
    private final DocumentService documentService;

    //@Override
    @Transactional
    public Case createCase(Long userId, CreateCaseDto dto) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        // Enhanced validation with configuration data
        /* TODO
        *   Re-Enable validation function()*/
        //validateCaseSubmission(patient, dto);

        // Validate subscription
        if (patient.getAccountLocked() || patient.getSubscriptionStatus() != SubscriptionStatus.ACTIVE) {
            throw new BusinessException("Active subscription required", HttpStatus.PAYMENT_REQUIRED);
        }

        Case medicalCase = Case.builder()
                .patient(patient)
                .caseTitle(dto.getCaseTitle())
                .description(dto.getDescription())
                .primaryDiseaseCode(dto.getPrimaryDiseaseCode())
                .secondaryDiseaseCodes(dto.getSecondaryDiseaseCodes())
                .symptomCodes(dto.getSymptomCodes())
                .currentMedicationCodes(dto.getCurrentMedicationCodes())
                .requiredSpecialization(dto.getRequiredSpecialization())
                .secondarySpecializations(dto.getSecondarySpecializations())
                .status(SUBMITTED)
                .urgencyLevel(dto.getUrgencyLevel())
                .complexity(dto.getComplexity())
                .paymentStatus(PaymentStatus.PENDING)
                .requiresSecondOpinion(dto.getRequiresSecondOpinion())
                .minDoctorsRequired(dto.getMinDoctorsRequired())
                .maxDoctorsAllowed(dto.getMaxDoctorsAllowed())
                .submittedAt(LocalDateTime.now())
                .assignmentAttempts(0)
                .rejectionCount(0)
                .build();

        Case saved = caseRepository.save(medicalCase);

        // Process and save uploaded files
        if (dto.getFiles() != null && !dto.getFiles().isEmpty()) {
            try {
                List<Document> documents = documentService.processAndSaveFiles(
                        dto.getFiles(), medicalCase, userId);

                log.info("Successfully processed {} files for case {}",
                        documents.size(), medicalCase.getId());
            } catch (Exception e) {
                log.error("Error processing files for case {}: {}", medicalCase.getId(), e.getMessage(), e);
                // Delete the case if file processing fails
                caseRepository.delete(medicalCase);
                throw new BusinessException("Failed to process uploaded files: " + e.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        // Update case status to PENDING and trigger smart assignment
        saved.setStatus(CaseStatus.PENDING);
        caseRepository.save(saved);

        // Trigger smart case assignment asynchronously
        try {
            // 🔥 NEW: Send Kafka event after saving new case to trigger SmartCaseAssignmentService
            patientEventProducer.sendStartSmartCaseAssignmentService(saved.getId());
        } catch (Exception e) {
            System.out.println("Failed to assign case automatically: " +  saved.getId());
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        return saved;
    }

    private void validateCaseSubmission(Patient patient, CreateCaseDto dto) {
        // Validate subscription
        if (patient.getSubscriptionStatus() != SubscriptionStatus.ACTIVE) {
            throw new BusinessException("Active subscription required", HttpStatus.PAYMENT_REQUIRED);
        }

        // Validate disease code exists
        try {
            configService.findDiseaseByIcdCodeCustom(dto.getPrimaryDiseaseCode());
        } catch (Exception e) {
            throw new BusinessException("Invalid primary disease code", HttpStatus.BAD_REQUEST);
        }

        // Validate specialization matches disease requirements
        List<String> requiredSpecs = configService.getSpecializationsForDisease(dto.getPrimaryDiseaseCode());
        if (!requiredSpecs.isEmpty() && !requiredSpecs.contains(dto.getRequiredSpecialization())) {
            throw new BusinessException("Specialization does not match disease requirements", HttpStatus.BAD_REQUEST);
        }

        // Validate all secondary disease codes
        for (String diseaseCode : dto.getSecondaryDiseaseCodes()) {
            try {
                configService.getDiseaseByCode(diseaseCode);
            } catch (Exception e) {
                throw new BusinessException("Invalid secondary disease code: " + diseaseCode, HttpStatus.BAD_REQUEST);
            }
        }
    }


    @Transactional
    public PatientProfileDto createProfile(Long userId, PatientProfileDto dto) {
        if (patientRepository.existsByUserId(userId)) {
            throw new BusinessException("Patient profile already exists", HttpStatus.CONFLICT);
        }

        Patient patient = Patient.builder()
                .userId(userId)
                .fullName(dto.getFullName())
                .dateOfBirth(dto.getDateOfBirth())
                .gender(dto.getGender())
                .medicalHistory(dto.getMedicalHistory())
                .subscriptionStatus(SubscriptionStatus.PENDING)
                .accountLocked(true) // Locked until subscription payment
                .phoneNumber(dto.getPhoneNumber())
                .address(dto.getAddress())
                .city(dto.getCity())
                .country(dto.getCountry())
                .postalCode(dto.getPostalCode())
                .emergencyContactName(dto.getEmergencyContactName())
                .emergencyContactPhone(dto.getEmergencyContactPhone())
                .bloodGroup(dto.getBloodGroup())
                .allergies(dto.getAllergies())
                .chronicConditions(dto.getChronicConditions())
                .casesSubmitted(0)
                .build();

        Patient saved = patientRepository.save(patient);
        return mapToDto(saved);
    }

    @Transactional
    public SubscriptionDto createSubscription(Long userId, SubscriptionDto dto) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        // Set subscription amount based on plan type
        BigDecimal amount = calculateSubscriptionAmount(dto.getPlanType());

        Subscription subscription = Subscription.builder()
                .patient(patient)
                .planType(dto.getPlanType())
                .amount(amount)
                .startDate(LocalDateTime.now())
                .endDate(calculateEndDate(dto.getPlanType()))
                .paymentStatus(PaymentStatus.PENDING)
                .paymentMethod(dto.getPaymentMethod())
                .autoRenew(dto.getAutoRenew() != null ? dto.getAutoRenew() : true)
                .build();

        Subscription saved = subscriptionRepository.save(subscription);

        // Simulate payment processing
        processPayment(saved, patient);

        // 🔥 NEW: Send Kafka event after subscription creation
        patientEventProducer.sendSubscriptionCreatedEvent(
                patient.getId(),
                dto.getPlanType().toString(),
                amount.doubleValue()
        );

        dto.setId(saved.getId());
        dto.setAmount(amount);
        return dto;
    }

//    @Transactional
//    public Case createCase(Long userId, CreateCaseDto dto) {
//        Patient patient = patientRepository.findByUserId(userId)
//                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));
//
//        // Check subscription status
//        if (patient.getSubscriptionStatus() != SubscriptionStatus.ACTIVE) {
//            throw new BusinessException("Active subscription required to submit cases", HttpStatus.FORBIDDEN);
//        }
//
//        if (patient.getAccountLocked()) {
//            throw new BusinessException("Account is locked. Please complete subscription payment", HttpStatus.FORBIDDEN);
//        }
//
//        Case medicalCase = Case.builder()
//                .patient(patient)
//                .caseTitle(dto.getCaseTitle())
//                .description(dto.getDescription())
//                //.category(dto.getCategory())
//                //.subCategory(dto.getSubCategory())
//                .status(CaseStatus.SUBMITTED)
//                .urgencyLevel(dto.getUrgencyLevel())
//                .paymentStatus(PaymentStatus.PENDING)
//                .rejectionCount(0)
//                .build();
//
//        Case saved = caseRepository.save(medicalCase);
//
//        // Update patient's case count
//        patient.setCasesSubmitted(patient.getCasesSubmitted() + 1);
//        patientRepository.save(patient);
//
//        // Move to PENDING status for doctor assignment
//        saved.setStatus(CaseStatus.PENDING);
//        return caseRepository.save(saved);
//    }

    public List<CaseDto> getDoctorActiveCases (Long doctorId ){
        List<CaseDto> cases = new ArrayList<>();
        List<Case> tempCases = new ArrayList<>();
        List<CaseAssignment> assignments = new ArrayList<>();

        assignments = caseAssignmentRepository.findByDoctorIdAndStatus( doctorId, AssignmentStatus.ACCEPTED );
        System.out.println("Getting assignments for doctor: " + doctorId);
        System.out.println("Doctor assignments count: " + assignments.size());
        if( assignments != null && !assignments.isEmpty() ){
            //Get related active cases
            tempCases = new ArrayList<>(assignments.stream().map(CaseAssignment::getCaseEntity).
                    filter(caseEntity ->
                            caseEntity.getStatus().equals(ACCEPTED) ||
                                    caseEntity.getStatus().equals(SCHEDULED) ||
                                    caseEntity.getStatus().equals(PAYMENT_PENDING) ||
                                    caseEntity.getStatus().equals(IN_PROGRESS)).
                    toList());
            System.out.println("tempCases (active cases) count: " + tempCases.size());

            if( tempCases != null && !tempCases.isEmpty() ){
                cases = tempCases.stream().map(this ::convertToCaseDto ).collect(Collectors.toList());
                System.out.println("Doctor active cases count: " + cases.size());
            }
        }
        return cases;
    }

    public List<CaseDto> getPatientCases(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        return caseRepository.findByPatientIdAndIsDeletedFalse(patient.getId()).stream().map(this::convertToCaseDto).collect(Collectors.toList());
    }

    public List<CaseDto> getAssignedCasesForDoctor(Long doctorId) {
        List<CaseDto> cases = new ArrayList<>();
        List<CaseAssignment> assignments;

        assignments = caseAssignmentRepository.findByDoctorIdAndStatus( doctorId, AssignmentStatus.PENDING );
        System.out.println("Getting assignments for doctor: " + doctorId);
        System.out.println("Doctor assignments count: " + assignments.size());
        if(!assignments.isEmpty()){
            //Get related ASSIGNED cases
            List<Case> tempCases = new ArrayList<>(assignments.stream().map(CaseAssignment::getCaseEntity).
                    filter(caseEntity ->
                    caseEntity.getStatus().equals(ASSIGNED)).
                    toList());
            System.out.println("tempCases assignments count: " + tempCases.size());

            if(!tempCases.isEmpty()){
                cases = tempCases.stream().map(this ::convertToCaseDto ).collect(Collectors.toList());
                System.out.println("Doctor assigned cases count: " + cases.size());
            }
        }
        return cases;
    }

    public List<CaseDto> getCasesPool(String specialization){
        List<CaseDto> caseDtos = new ArrayList<>();
        caseDtos = caseRepository.findCaseByRequiredSpecializationAndStatusAndIsDeletedFalse(specialization,
                        CaseStatus.PENDING).stream().map(this::convertToCaseDto).toList();
        return caseDtos;
    }

    public CaseDto convertToCaseDto(Case newCase){
        CaseDto caseDto = new CaseDto();
        ModelMapper modelMapper=new ModelMapper();
        caseDto = modelMapper.map(newCase, CaseDto.class);
        return caseDto;
    }

    public PatientProfileDto getProfile(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        return mapToDto(patient);
    }

    @Transactional
    public void acceptAppointment(Long userId, Long caseId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        Case medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new BusinessException("Case not found", HttpStatus.NOT_FOUND));

        if (!medicalCase.getPatient().getId().equals(patient.getId())) {
            throw new BusinessException("Unauthorized access to case", HttpStatus.FORBIDDEN);
        }

        if (medicalCase.getStatus() != CaseStatus.SCHEDULED) {
            throw new BusinessException("Case is not in scheduled status", HttpStatus.BAD_REQUEST);
        }

        // Move to payment pending status
        medicalCase.setStatus(CaseStatus.PAYMENT_PENDING);
        caseRepository.save(medicalCase);
    }

    public AppointmentDto convertToAppointmentDto(AppointmentDto newAppointment){
        AppointmentDto appointmentDto = new AppointmentDto();
        appointmentDto.setCaseId(newAppointment.getCaseId());
        appointmentDto.setPatientId(newAppointment.getPatientId());
        appointmentDto.setDoctor(newAppointment.getDoctor());
        appointmentDto.setDuration(newAppointment.getDuration());
        appointmentDto.setScheduledTime(newAppointment.getScheduledTime());
        appointmentDto.setCompletedAt(newAppointment.getCompletedAt());
        appointmentDto.setStatus(newAppointment.getStatus());
        appointmentDto.setRescheduleCount(newAppointment.getRescheduleCount());
        appointmentDto.setMeetingId(newAppointment.getMeetingId());
        appointmentDto.setMeetingLink(newAppointment.getMeetingLink());
        appointmentDto.setRescheduledFrom(newAppointment.getRescheduledFrom());
        appointmentDto.setRescheduleReason(newAppointment.getRescheduleReason());
        appointmentDto.setCompletedAt(newAppointment.getCompletedAt());
        return appointmentDto;
    }

    public List<AppointmentDto> getPatientAppointments(Long patientId) {
        List<AppointmentDto> patientAppointments = new ArrayList<>();
        try{
            patientAppointments = doctorServiceClient.getPatientAppointments(patientId).getBody().getData();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return patientAppointments;
    }

    @Transactional
    public void payConsultationFee(Long userId, ProcessPaymentDto paymentDto ) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        Case medicalCase = caseRepository.findById(paymentDto.getCaseId())
                .orElseThrow(() -> new BusinessException("Case not found", HttpStatus.NOT_FOUND));

        if (!medicalCase.getPatient().getId().equals(patient.getId())) {
            throw new BusinessException("Unauthorized access to case", HttpStatus.FORBIDDEN);
        }

        if (medicalCase.getStatus() != CaseStatus.PAYMENT_PENDING) {
            throw new BusinessException("Payment not required at this stage", HttpStatus.BAD_REQUEST);
        }

        // Simulate payment processing first:

         PaymentDto updatedPayment =  paymentServiceClient.processPayment(paymentDto).getBody().getData();

        //update case status:
        if( updatedPayment!=null ){
            medicalCase.setPaymentStatus(PaymentStatus.COMPLETED);
            medicalCase.setStatus(IN_PROGRESS);
            caseRepository.save(medicalCase);

            //Update Appointment status to be CONFIRMED
            doctorServiceClient.confirmAppointment(updatedPayment.getCaseId(), updatedPayment.getPatientId(),
                    updatedPayment.getDoctorId());
        }
    }

    private void processPayment(Subscription subscription, Patient patient) {
        // Simulate payment processing
        subscription.setPaymentStatus(PaymentStatus.COMPLETED);
        subscription.setTransactionId("TXN_" + System.currentTimeMillis());
        subscriptionRepository.save(subscription);

        // Update patient subscription status
        patient.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        patient.setSubscriptionExpiry(subscription.getEndDate());
        patient.setSubscriptionPaymentDate(LocalDateTime.now());
        patient.setAccountLocked(false);
        patientRepository.save(patient);
    }

    private BigDecimal calculateSubscriptionAmount(PlanType planType) {
        return switch (planType) {
            case BASIC -> new BigDecimal("29.99");
            case PREMIUM -> new BigDecimal("59.99");
            case PRO -> new BigDecimal("99.99");
        };
    }

    private LocalDateTime calculateEndDate(PlanType planType) {
        return switch (planType) {
            case BASIC -> LocalDateTime.now().plusMonths(1);
            case PREMIUM -> LocalDateTime.now().plusMonths(3);
            case PRO -> LocalDateTime.now().plusMonths(6);
        };
    }

    private PatientProfileDto mapToDto(Patient patient) {
        PatientProfileDto dto = new PatientProfileDto();
        dto.setId(patient.getId());
        dto.setUserId(patient.getUserId());
        dto.setFullName(patient.getFullName());
        dto.setDateOfBirth(patient.getDateOfBirth());
        dto.setGender(patient.getGender());
        dto.setMedicalHistory(patient.getMedicalHistory());
        dto.setSubscriptionStatus(patient.getSubscriptionStatus());
        dto.setEmergencyContactName(patient.getEmergencyContactName());
        dto.setEmergencyContactPhone(patient.getEmergencyContactPhone());
        dto.setBloodGroup(patient.getBloodGroup());
        dto.setAllergies(patient.getAllergies());
        dto.setChronicConditions(patient.getChronicConditions());
        dto.setPhoneNumber(patient.getPhoneNumber());
        dto.setAddress(patient.getAddress());
        dto.setCity(patient.getCity());
        dto.setCountry(patient.getCountry());
        dto.setPostalCode(patient.getPostalCode());
        return dto;
    }

    public CustomPatientDto getCustomPatientInformation( Long caseId, Long doctorId ){
        CustomPatientDto customPatientDto = new CustomPatientDto();
        //Check if the Case Existed:
        Case medicalCase = caseRepository.findByIdAndIsDeletedFalse(caseId)
                .orElseThrow(() -> new BusinessException("Case not found", HttpStatus.NOT_FOUND));

        //check case assignment if case belongs to the provided doctorId:
        CaseAssignment assignments = caseAssignmentRepository.findByCaseEntityAndDoctorId(medicalCase,
                doctorId).orElse(null);
        if(assignments == null){
            throw new BusinessException("No match between Doctor and Provided Case", HttpStatus.NOT_FOUND);
        }
        else{
            Patient patient = patientRepository.findById(medicalCase.getPatient().getId()).
                    orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));
            ModelMapper modelMapper = new ModelMapper();
            customPatientDto = modelMapper.map(patient, CustomPatientDto.class);
        }
        return customPatientDto;
    }

    /**
     * Update case consultation fee (called via Kafka event)
     */
    @Transactional
    public void updateCaseConsultationFee(Long caseId, BigDecimal consultationFee, LocalDateTime feeSetAt) {
        try {
            Case medicalCase = caseRepository.findById(caseId)
                    .orElseThrow(() -> new BusinessException("Case not found", HttpStatus.NOT_FOUND));

            // Validate case status
            if (medicalCase.getStatus() != CaseStatus.ACCEPTED) {
                log.warn("Attempting to set fee for case {} with status: {}", caseId, medicalCase.getStatus());
                // Don't throw exception, just log warning as this is async processing
                return;
            }

            // Update consultation fee
            medicalCase.setConsultationFee(consultationFee);
            medicalCase.setFeeSetAt(feeSetAt);

            // Save updated case
            caseRepository.save(medicalCase);

            log.info("Consultation fee updated for case {}: ${}", caseId, consultationFee);

        } catch (Exception e) {
            log.error("Error updating consultation fee for case {}: {}", caseId, e.getMessage(), e);
            throw e;
        }
    }

    // 1. Update Profile Implementation
    @Transactional
    public PatientProfileDto updateProfile(Long userId, PatientProfileDto dto) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        // Update only provided fields
        if (dto.getPhoneNumber() != null) patient.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getAddress() != null) patient.setAddress(dto.getAddress());
        if (dto.getCity() != null) patient.setCity(dto.getCity());
        if (dto.getCountry() != null) patient.setCountry(dto.getCountry());
        if (dto.getPostalCode() != null) patient.setPostalCode(dto.getPostalCode());
        if (dto.getEmergencyContactName() != null) patient.setEmergencyContactName(dto.getEmergencyContactName());
        if (dto.getEmergencyContactPhone() != null) patient.setEmergencyContactPhone(dto.getEmergencyContactPhone());
        if (dto.getAllergies() != null) patient.setAllergies(dto.getAllergies());
        if (dto.getChronicConditions() != null) patient.setChronicConditions(dto.getChronicConditions());
        if (dto.getMedicalHistory() != null) patient.setMedicalHistory(dto.getMedicalHistory());

        Patient updated = patientRepository.save(patient);
        return mapToDto(updated);
    }

    // 2. Get Subscription Status Implementation
    public SubscriptionStatusDto getSubscriptionStatus(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        Subscription activeSubscription = subscriptionRepository
                .findTopByPatientIdOrderByCreatedAtDesc(patient.getId())
                .orElse(null);

        SubscriptionStatusDto status = new SubscriptionStatusDto();


        if (activeSubscription != null) {
            status.setStatus(patient.getSubscriptionStatus());
            status.setExpiryDate(patient.getSubscriptionExpiry());
            status.setIsActive(patient.getSubscriptionStatus() == SubscriptionStatus.ACTIVE);
            status.setPlanType(activeSubscription.getPlanType());
            status.setAmount(activeSubscription.getAmount());
            status.setAutoRenew(activeSubscription.getAutoRenew());
            status.setPaymentMethod(activeSubscription.getPaymentMethod());
            status.setCreatedAt(activeSubscription.getCreatedAt());
        }
        else
            throw new BusinessException("You have No Active Subscription ...", HttpStatus.NOT_FOUND);

        return status;
    }

    // 3. Get Case Details Implementation
    public CaseDetailsDto getCaseDetails(Long userId, Long caseId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        Case medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new BusinessException("Case not found", HttpStatus.NOT_FOUND));

        if (!medicalCase.getPatient().getId().equals(patient.getId())) {
            throw new BusinessException("Unauthorized access to case", HttpStatus.FORBIDDEN);
        }

        CaseDetailsDto details = new CaseDetailsDto();
        details.setId(medicalCase.getId());
        details.setCaseTitle(medicalCase.getCaseTitle());
        details.setDescription(medicalCase.getDescription());
        //details.setCategory(medicalCase.getCategory());
        //details.setSubCategory(medicalCase.getSubCategory());
        details.setStatus(medicalCase.getStatus());
        details.setUrgencyLevel(medicalCase.getUrgencyLevel());
        //details.setAssignedDoctorId(medicalCase.getAssignedDoctorId());
        //details.setConsultationFee(medicalCase.getConsultationFee());
        details.setPaymentStatus(medicalCase.getPaymentStatus());
        details.setCreatedAt(medicalCase.getCreatedAt());
        //details.setAcceptedAt(medicalCase.getAcceptedAt());
        //details.setScheduledAt(medicalCase.getScheduledAt());
        //details.setPaymentCompletedAt(medicalCase.getPaymentCompletedAt());
        //details.setClosedAt(medicalCase.getClosedAt());
        //details.setRejectionReason(medicalCase.getRejectionReason());

        details.setPrimaryDiseaseCode(medicalCase.getPrimaryDiseaseCode());
        details.setSecondaryDiseaseCodes(medicalCase.getSecondaryDiseaseCodes());
        details.setSymptomCodes(medicalCase.getSymptomCodes());
        details.setCurrentMedicationCodes(medicalCase.getCurrentMedicationCodes());
        details.setRequiredSpecialization(medicalCase.getRequiredSpecialization());
        details.setSecondarySpecializations(medicalCase.getSecondarySpecializations());

        // Add documents if any
        List<Document> documents = documentRepository.findByCaseId(caseId);
        details.setDocuments(documents);

        return details;
    }

    // 4. Request Reschedule Implementation
    @Transactional
    public void requestReschedule(Long userId, Long caseId, RescheduleRequestDto dto) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        Case medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new BusinessException("Case not found", HttpStatus.NOT_FOUND));

        if (!medicalCase.getPatient().getId().equals(patient.getId())) {
            throw new BusinessException("Unauthorized access to case", HttpStatus.FORBIDDEN);
        }

        if (medicalCase.getStatus() != CaseStatus.SCHEDULED) {
            throw new BusinessException("Case is not in scheduled status", HttpStatus.BAD_REQUEST);
        }

        RescheduleRequest request = RescheduleRequest.builder()
                .caseId(caseId)
                .requestedBy("PATIENT")
                .reason(dto.getReason())
                .preferredTimes(String.join(",", dto.getPreferredTimes()))
                .status(RescheduleStatus.PENDING)
                .build();

        rescheduleRequestRepository.save(request);

        Long doctorId = -1L;
        try{
            doctorId = medicalCase.getAssignments().get(0).getDoctorId();
            // 🔥 NEW: Send Kafka event instead of direct notification
            patientEventProducer.sendRescheduleRequestEvent(
                    caseId, patient.getId(), doctorId, dto.getReason());
        } catch (Exception e) {
            log.error("Doctor not found ...");
            log.error(e.getMessage());
        }
    }

    // 5. Get Payment History Implementation
    public List<PaymentHistoryDto> getPaymentHistory(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        List<PaymentHistoryDto> paymentHistoryDtoList = new ArrayList<>();
        try{
            paymentHistoryDtoList = paymentServiceClient.getPatientPaymentHistory(patient.getId()).getBody().getData();
        }catch(Exception e) {
            paymentHistoryDtoList = null;
            e.printStackTrace();
        }

        return paymentHistoryDtoList;
    }

    // 6. Update Case Status Implementation (For internal use by other services)
    /*TODO
    *  must check here or before the payment status (validation)*/
    @Transactional
    public void updateCaseStatus(Long caseId, String status, Long doctorId) {
        Case medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new BusinessException("Case not found", HttpStatus.NOT_FOUND));

        CaseStatus newStatus = CaseStatus.valueOf(status);
        String oldStatus = medicalCase.getStatus().toString();
        medicalCase.setStatus(newStatus);

        CaseAssignment caseAssignment = medicalCase.getAssignments().
                stream().filter(p ->
                        p.getDoctorId().equals(doctorId)).findFirst().orElse(null);
        if( caseAssignment != null ){
            CaseAssignment updatedCaseAssignment;
            updatedCaseAssignment = caseAssignment;
            if( newStatus == ACCEPTED ){
                updatedCaseAssignment.setStatus(AssignmentStatus.ACCEPTED);
            }
            else if( newStatus == REJECTED){
                updatedCaseAssignment.setStatus(AssignmentStatus.PENDING);
            }
            else if( newStatus == IN_PROGRESS){
                medicalCase.setPaymentStatus(PaymentStatus.COMPLETED);
            }
            else if (newStatus == CaseStatus.CONSULTATION_COMPLETE || newStatus == CaseStatus.CLOSED) {
                medicalCase.setClosedAt(LocalDateTime.now());
            }
            else if( updatedCaseAssignment.getExpiresAt().isBefore(LocalDateTime.now()) ){
                updatedCaseAssignment.setStatus(AssignmentStatus.EXPIRED);
            }

            caseAssignmentRepository.save(updatedCaseAssignment);
        }

        caseRepository.save(medicalCase);

        // 🔥 NEW: Send Kafka event instead of direct notification
        patientEventProducer.sendCaseStatusUpdateEvent(
                caseId, oldStatus, status,
                medicalCase.getPatient().getId(), doctorId
        );
    }

    @Transactional
    public void updateCase( Long caseId, UpdateCaseDto updatedCase ){
        Case medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new BusinessException("Case not found", HttpStatus.NOT_FOUND));
        //Check if Case Status allows the update (must be: )
        if( medicalCase.getStatus() == SUBMITTED || medicalCase.getStatus() == PENDING ){

            if(updatedCase.getCaseTitle() != null)
                medicalCase.setCaseTitle(updatedCase.getCaseTitle());
            if(updatedCase.getDescription() != null)
                medicalCase.setDescription(updatedCase.getDescription());
            if(updatedCase.getPrimaryDiseaseCode() != null)
                medicalCase.setPrimaryDiseaseCode(updatedCase.getPrimaryDiseaseCode());
            if(updatedCase.getSecondaryDiseaseCodes() != null)
                medicalCase.setSecondaryDiseaseCodes(updatedCase.getSecondaryDiseaseCodes());
            if(updatedCase.getSymptomCodes() != null)
                medicalCase.setSymptomCodes(updatedCase.getSymptomCodes());
            if(updatedCase.getCurrentMedicationCodes() != null)
                medicalCase.setCurrentMedicationCodes(updatedCase.getCurrentMedicationCodes());
            if(updatedCase.getRequiredSpecialization() != null)
                medicalCase.setRequiredSpecialization(updatedCase.getRequiredSpecialization());
            if(updatedCase.getSecondarySpecializations() != null)
                medicalCase.setSecondarySpecializations(updatedCase.getSecondarySpecializations());
            if(updatedCase.getUrgencyLevel() != null)
                medicalCase.setUrgencyLevel(updatedCase.getUrgencyLevel());
            if(updatedCase.getComplexity() != null)
                medicalCase.setComplexity(updatedCase.getComplexity());

            // Commit the update:
            caseRepository.save(medicalCase);
        }
        else{
            throw new BusinessException("Current Case Status doesn't allow the update", HttpStatus.UNAUTHORIZED);
        }
    }

    @Transactional
    public void acceptAssignment(Long doctorId, Long assignmentId){
        CaseAssignment caseAssignment =  caseAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException("Assignment not found", HttpStatus.NOT_FOUND));
        if (!caseAssignment.getDoctorId().equals(doctorId)) {
            throw new BusinessException("Unauthorized access", HttpStatus.FORBIDDEN);
        }

        if (caseAssignment.getStatus() != AssignmentStatus.PENDING) {
            throw new BusinessException("Assignment cannot be accepted", HttpStatus.BAD_REQUEST);
        }

        String oldStatus = caseAssignment.getCaseEntity().getStatus().toString();
        caseAssignment.setStatus(AssignmentStatus.ACCEPTED);
        caseAssignment.setRespondedAt(LocalDateTime.now());
        caseAssignmentRepository.save(caseAssignment);

        // Update case status if this is the primary assignment
        if (caseAssignment.getPriority() == AssignmentPriority.PRIMARY) {
            Case medicalCase = caseAssignment.getCaseEntity();
            medicalCase.setStatus(ACCEPTED);
            caseRepository.save(medicalCase);
            String status = medicalCase.getStatus().toString();
            //Send Kafka Notification for the patient that his case was assigned and accepted by the doctor
            patientEventProducer.sendCaseStatusUpdateEvent(
                    medicalCase.getId(), oldStatus, status,
                    medicalCase.getPatient().getId(), doctorId
            );
        }
    }

    @Transactional
    public void claimCase(Long caseId, Long doctorId, String note){
        try {
            log.info("Doctor {} attempting to claim case {}", doctorId, caseId);

            Case claimedCase =  caseRepository.findById(caseId)
                    .orElseThrow(() -> new BusinessException("Case not found", HttpStatus.NOT_FOUND));

            if (claimedCase.getStatus() != CaseStatus.PENDING) {
                throw new BusinessException("Case cannot be accepted", HttpStatus.BAD_REQUEST);
            }

            claimedCase.setStatus(CaseStatus.ASSIGNED);
            claimedCase.setFirstAssignedAt(LocalDateTime.now());
            caseRepository.save(claimedCase);

            //update CaseAssignment Relation Table
            try{
                createOrUpdateCaseAssignment(caseId, doctorId, note);
            }catch(Exception e){
                e.printStackTrace();
            }

            log.info("Case {} successfully claimed by doctor {}", caseId, doctorId);
        } catch (BusinessException e) {
            log.warn("Business rule violation in case claiming: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error claiming case {} by doctor {}: {}",
                    caseId, doctorId, e.getMessage(), e);
            throw new BusinessException("Unable to claim case at this time",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void createOrUpdateCaseAssignment(Long caseId, Long doctorId, String note) {
        List<CaseAssignment> existingAssignments = caseAssignmentRepository.findByCaseEntityId(caseId);

        CaseAssignment assignment;
        if (existingAssignments.isEmpty()) {
            assignment = CaseAssignment.builder()
                    .caseEntity(caseRepository.getReferenceById(caseId))
                    .doctorId(doctorId)
                    .assignedAt(LocalDateTime.now())
                    .assignmentReason(note)
                    .status(AssignmentStatus.ACCEPTED)
                    .build();
        } else {
            assignment = existingAssignments.get(0);
            assignment.setDoctorId(doctorId);
            assignment.setAssignedAt(LocalDateTime.now());
            assignment.setAssignmentReason(note);
            assignment.setStatus(AssignmentStatus.ACCEPTED);
        }

        caseAssignmentRepository.save(assignment);
    }

    @Transactional
    public void rejectAssignment(Long doctorId, Long assignmentId, String reason){
        CaseAssignment assignment =  caseAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException("Assignment not found", HttpStatus.NOT_FOUND));
        if (!assignment.getDoctorId().equals(doctorId)) {
            throw new BusinessException("Unauthorized access", HttpStatus.FORBIDDEN);
        }

        if (assignment.getStatus() != AssignmentStatus.PENDING) {
            throw new BusinessException("Assignment cannot be rejected", HttpStatus.BAD_REQUEST);
        }

        assignment.setStatus(AssignmentStatus.REJECTED);
        assignment.setRejectionReason(reason);
        assignment.setRespondedAt(LocalDateTime.now());
        caseAssignmentRepository.save(assignment);

        // 🔥 Send Kafka event after saving new case to trigger SmartCaseAssignmentService
        patientEventProducer.sendStartSmartCaseAssignmentService(assignment.getCaseEntity().getId());

    }

    public static CaseAssignmentDto assignmentDtoCovert(CaseAssignment assignment) {
        CaseAssignmentDto dto = new CaseAssignmentDto();
        dto.setId(assignment.getId());
        dto.setAssignedAt(assignment.getAssignedAt());
        dto.setStatus(assignment.getStatus());
        dto.setAssignmentReason(assignment.getAssignmentReason());
        dto.setPriority(assignment.getPriority());
        dto.setCaseId(assignment.getCaseEntity().getId());
        dto.setRejectionReason(assignment.getRejectionReason());
        dto.setRespondedAt(assignment.getRespondedAt());
        dto.setExpiresAt(assignment.getExpiresAt());
        dto.setMatchingScore(assignment.getMatchingScore());
        return dto;
    }

    public Map<String,Long> getAllCassesMetrics(){
        Map<String,Long> metrics = new HashMap<>();
        List<CaseStatus> activeStatusList = new ArrayList<>();
        activeStatusList.add(CaseStatus.PENDING);
        activeStatusList.add(CaseStatus.SUBMITTED);
        activeStatusList.add(CaseStatus.ASSIGNED);
        activeStatusList.add(ACCEPTED);
        activeStatusList.add(CaseStatus.SCHEDULED);
        activeStatusList.add(CaseStatus.PAYMENT_PENDING);
        activeStatusList.add(IN_PROGRESS);
        activeStatusList.add(CONSULTATION_COMPLETE);
        activeStatusList.add(CaseStatus.REJECTED);

        Long activeCasesCount = caseRepository.countByStatusInAndIsDeletedFalse(activeStatusList);
        Long completedCasesCount = caseRepository.countByStatusAndIsDeletedFalse(CONSULTATION_COMPLETE);
        Long inProgressCasesCunt = caseRepository.countByStatusAndIsDeletedFalse(IN_PROGRESS);
        Long activeSubscriptionsCount = patientRepository.countBySubscriptionStatus(SubscriptionStatus.ACTIVE);
        //Long averageCaseResolutionTime = Long.valueOf(caseRepository.calculateAverageResolutionTime());

        activeStatusList.add(CaseStatus.CLOSED);
        Long totalCasesCount = caseRepository.countByStatusInAndIsDeletedFalse(activeStatusList);

        metrics.put("totalCasesCount", totalCasesCount);
        metrics.put("activeCasesCount", activeCasesCount);
        metrics.put("completedCasesCount", completedCasesCount);
        metrics.put("inProgressCasesCunt", inProgressCasesCunt);
        metrics.put("activeSubscriptionsCount", activeSubscriptionsCount);
        //metrics.put("averageCaseResolutionTime", averageCaseResolutionTime);

        return metrics;
    }

    public NotificationDto convertToNotficationDto(NotificationDto notification){
        NotificationDto dto = new NotificationDto();
        ModelMapper modelMapper = new ModelMapper();
        dto = modelMapper.map(notification, NotificationDto.class);
        return dto;
    }

    public PatientDashboardDto getPatientDashboard(Long userId){

        /*TODO
          below lines must be deleted, the query for the patient, and get its ID to be used to get the appointments.
         */
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        PatientDashboardDto dto = new PatientDashboardDto();
        try{
            List<CaseStatus> statusList = new ArrayList<>();
            StatsDto stats = new StatsDto();
            statusList.add(ACCEPTED);
            statusList.add(ASSIGNED);
            statusList.add(IN_PROGRESS);
            statusList.add(SCHEDULED);
            statusList.add(CONSULTATION_COMPLETE);
            stats.setTotalCases( caseRepository.countByPatientIdAndIsDeletedFalse(patient.getId()) );
            stats.setActiveCases( caseRepository.countByStatusInAndPatientIdAndIsDeletedFalse(statusList, patient.getId()) );
            stats.setCompletedCases(caseRepository.countByPatientIdAndStatusAndIsDeletedFalse(patient.getId() ,CLOSED));
            dto.setStats(stats);
            List<Case> recentCases = caseRepository.findLastSubmittedCases(patient.getId(), 3);
            dto.setRecentCases(recentCases.stream().map(this::convertToCaseDto).toList());
            dto.setUpcomingAppointments(getPatientAppointments(userId));
            stats.setUpcomingAppointments(dto.getUpcomingAppointments().stream().count());
            List<NotificationDto> recentNotifications = getMyNotifications(userId);
            dto.setRecentNotifications(recentNotifications);
        }catch(Exception e){
            log.error("Failed to get patient dashboard");
            log.error(e.getMessage());
            e.printStackTrace();
        }
        return dto;
    }

    public List<NotificationDto> getMyNotifications(Long userId){
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));
        List<NotificationDto> dtos = new ArrayList<>();
        try{
            dtos = notificationServiceClient.getUserNotifications(patient.getId()).getBody().getData();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return dtos;
    }

    @Transactional
    public void deleteCase(Long caseId, Long userId){
        //validate if case existed:
        Case medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new BusinessException("Case not found", HttpStatus.NOT_FOUND));
        //validate if case belongs to the provided user and that user existed:
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        if(!Objects.equals(medicalCase.getPatient().getId(), patient.getId())){
            throw new BusinessException("You are not authorized to deleted this case",
                            HttpStatus.UNAUTHORIZED);
        }
        //validate if case eligible for delete
        if( medicalCase.getStatus().equals(SUBMITTED) || medicalCase.getStatus().equals(REJECTED)
                ||  medicalCase.getStatus().equals(PENDING) ){

            //Handel delete case
            //1- remove it from assignments
            List<CaseAssignment> caseAssignments = medicalCase.getAssignments();
            if(caseAssignments != null && !caseAssignments.isEmpty()){
                caseAssignmentRepository.deleteAll(caseAssignments);
            }
            //2- update case isDeleted field
            medicalCase.setIsDeleted(true);
            caseRepository.save(medicalCase);
        }
        else{
            throw new BusinessException("Case can not be deleted because its status preventing that.",
                    HttpStatus.UNAUTHORIZED);
        }
    }

    @Transactional
    public void activateSubscriptionAfterPayment(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        // Update subscription status
        patient.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        patient.setAccountLocked(false);
        patientRepository.save(patient);

        log.info("Subscription activated for patient: {}", patientId);
    }

    @Transactional
    public void updateCaseAfterPayment(Long caseId, Long patientId) {
        Case medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new BusinessException("Case not found", HttpStatus.NOT_FOUND));

        // Update case to indicate payment is complete
        medicalCase.setPaymentStatus(PaymentStatus.COMPLETED);
        medicalCase.setStatus(IN_PROGRESS);
        caseRepository.save(medicalCase);

        log.info("Case payment completed for case: {}", caseId);
    }

    @Transactional
    public void initializePatientProfile(Long userId, String email, String fullName) {
        // Check if patient profile already exists
        if (patientRepository.findByUserId(userId).isPresent()) {
            log.info("Patient profile already exists for user: {}", userId);
            System.out.println("Patient profile already exists for user: " + email);
            return;
        }

        // Create basic patient profile
        Patient patient = Patient.builder()
                .userId(userId)
                .email(email)
                .fullName(fullName)
                .subscriptionStatus(SubscriptionStatus.PENDING)
                .accountLocked(true)
                .casesSubmitted(0)
                .build();

        patientRepository.save(patient);
        System.out.println("Patient profile initialized for user: " + email);
        log.info("Patient profile initialized for user: {}", userId);
    }

    // Additional methods to add to PatientService.java

    /**
     * Enhanced getCaseDetailsWithFiles method with complete file access information
     */
    public CaseDetailsDto getCaseDetailsWithFiles(Long userId, Long caseId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        Case medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new BusinessException("Case not found", HttpStatus.NOT_FOUND));

        if (!medicalCase.getPatient().getId().equals(patient.getId())) {
            throw new BusinessException("Unauthorized access to case", HttpStatus.FORBIDDEN);
        }

        CaseDetailsDto details = new CaseDetailsDto();
        details.setId(medicalCase.getId());
        details.setCaseTitle(medicalCase.getCaseTitle());
        details.setDescription(medicalCase.getDescription());
        details.setStatus(medicalCase.getStatus());
        details.setUrgencyLevel(medicalCase.getUrgencyLevel());
        details.setPaymentStatus(medicalCase.getPaymentStatus());
        details.setCreatedAt(medicalCase.getCreatedAt());
//        details.setSubmittedAt(medicalCase.getSubmittedAt());
//        details.setFirstAssignedAt(medicalCase.getFirstAssignedAt());
//        details.setLastAssignedAt(medicalCase.getLastAssignedAt());
        details.setClosedAt(medicalCase.getClosedAt());

        // Get documents with enhanced access information
        List<Document> documents = documentService.getCaseDocuments(caseId, userId);
        details.setDocuments(documents);
        details.setDocumentCount(documents.size());

        // Calculate total document size
        long totalSize = documents.stream()
                .mapToLong(doc -> doc.getOriginalFileSize() != null ? doc.getOriginalFileSize().longValue() : 0L)
                .sum();
        details.setTotalDocumentSize(totalSize);

        // Create document access information
        List<CaseDetailsDto.DocumentAccessDto> documentAccess = documents.stream()
                .map(this::mapToDocumentAccess)
                .collect(Collectors.toList());
        details.setDocumentAccess(documentAccess);

        log.info("Case details retrieved with {} documents ({}KB total) for case: {} by user: {}",
                documents.size(), totalSize / 1024, caseId, userId);

        return details;
    }

    /**
     * Map Document entity to DocumentAccessDto for enhanced client access
     */
    private CaseDetailsDto.DocumentAccessDto mapToDocumentAccess(Document document) {
        CaseDetailsDto.DocumentAccessDto accessDto = new CaseDetailsDto.DocumentAccessDto();
        accessDto.setDocumentId(document.getId());
        accessDto.setFileName(document.getFileName());
        accessDto.setMimeType(document.getMimeType());
        accessDto.setFileSizeKB(document.getOriginalFileSize() != null ? document.getOriginalFileSize() / 1024 : 0);
        accessDto.setDocumentType(document.getDocumentType().name());
        accessDto.setAccessUrl(String.format("/api/files/%d", document.getId()));
        accessDto.setDownloadUrl(String.format("/api/patients/documents/%d/download", document.getId()));
        accessDto.setIsEncrypted(document.getIsEncrypted());
        accessDto.setIsCompressed(document.getIsCompressed());
        accessDto.setUploadedAt(document.getCreatedAt());
        accessDto.setDescription(document.getDescription());
        return accessDto;
    }

    // Add these methods to PatientService.java

    /**
     * Update case attachments - Add additional files to existing case
     */
    @Transactional
    public CaseAttachmentsDto updateCaseAttachments(Long userId, Long caseId, List<MultipartFile> files) {
        log.info("Updating case {} attachments for user {} with {} files", caseId, userId, files.size());

        // Validate user access to case and case state
        if(documentService.validateCaseFileAccess(caseId, userId)){
            // Validate patient profile and subscription
            Patient patient = patientRepository.findByUserId(userId)
                    .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

            if (patient.getAccountLocked() || patient.getSubscriptionStatus() != SubscriptionStatus.ACTIVE) {
                throw new BusinessException("Active subscription required to upload files", HttpStatus.PAYMENT_REQUIRED);
            }

            try {
                // Add files to the case
                List<Document> newDocuments = documentService.addFilesToCase(files, caseId, userId);

                // Create summary with new files information
                CaseAttachmentsDto result = documentService.createAttachmentsSummaryWithNewFiles(caseId,
                        userId, newDocuments);

                // Log the successful operation
                log.info("Successfully updated case {} with {} new attachments for user {}",
                        caseId, newDocuments.size(), userId);

                return result;

            } catch (BusinessException e) {
                log.warn("Business rule violation in case attachment update: {}", e.getMessage());
                throw e;
            } catch (Exception e) {
                log.error("Unexpected error updating case {} attachments for user {}: {}",
                        caseId, userId, e.getMessage(), e);
                throw new BusinessException("Unable to update case attachments at this time",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        else
            throw new BusinessException("Unable to update case attachments at this time",
                    HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Get case attachments summary
     */
    public CaseAttachmentsDto getCaseAttachments(Long userId, Long caseId) {
        log.info("Retrieving case {} attachments for user {}", caseId, userId);

        // Validate user access to case
        if(documentService.validateCaseFileAccess(caseId, userId)){
            System.out.println("Case: " + caseId + " validation for accessing its files done.");

            try {
                CaseAttachmentsDto attachments = documentService.getCaseAttachmentsSummary(caseId, userId);

                // Set case title from database
                Case medicalCase = caseRepository.findById(caseId)
                        .orElseThrow(() -> new BusinessException("Case not found", HttpStatus.NOT_FOUND));
                attachments.setCaseTitle(medicalCase.getCaseTitle());

                log.info("Retrieved {} attachments for case {} by user {}",
                        attachments.getTotalDocuments(), caseId, userId);

                return attachments;

            } catch (Exception e) {
                log.error("Error retrieving case {} attachments for user {}: {}",
                        caseId, userId, e.getMessage(), e);
                throw new BusinessException("Failed to retrieve case attachments", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        else{
            log.error("Access validation for case {} by user {} failed",
                    caseId, userId);
            throw new BusinessException("Failed to retrieve case attachments", HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }


    /*TODO Fix below send notification
    /**
     * Send notification when new files are uploaded to a case
     */
//    private void sendFileUploadNotification(Long caseId, Long userId, int fileCount) {
//        try {
//            Case medicalCase = caseRepository.findById(caseId).orElse(null);
//            if (medicalCase == null) {
//                log.warn("Case not found for notification: {}", caseId);
//                return;
//            }
//
//            NotificationDto notification = NotificationDto.builder()
//                    .receiverId(userId)
//                    .title("New Files Uploaded")
//                    .message(String.format("Successfully uploaded %d new file(s) to case '%s'",
//                            fileCount, medicalCase.getCaseTitle()))
//                    .type(NotificationType.CASE)
//                    .build();
//
//            notificationServiceClient.sendNotification(notification);
//
//            // Also notify assigned doctors if case is assigned
//            if (medicalCase.getStatus() == CaseStatus.ASSIGNED ||
//                    medicalCase.getStatus() == CaseStatus.ACCEPTED ||
//                    medicalCase.getStatus() == CaseStatus.IN_PROGRESS) {
//
//                List<CaseAssignment> assignments = caseAssignmentRepository.findByCaseEntityId(caseId);
//                for (CaseAssignment assignment : assignments) {
//                    if (assignment.getStatus() == AssignmentStatus.ACCEPTED) {
//                        NotificationDto doctorNotification = NotificationDto.builder()
//                                .receiverId(assignment.getDoctorId())
//                                .title("New Files Added to Case")
//                                .message(String.format("Patient uploaded %d new file(s) to case '%s'",
//                                        fileCount, medicalCase.getCaseTitle()))
//                                .type("CASE_FILE_UPDATE")
//                                .build();
//
//                        notificationServiceClient.sendNotification(doctorNotification);
//                    }
//                }
//            }
//
//        } catch (Exception e) {
//            log.error("Failed to send file upload notification for case {}: {}", caseId, e.getMessage(), e);
//        }
//    }

    /**
     * Validate if case allows file uploads based on its current status
     */
    private void validateCaseAllowsFileUploads(Case medicalCase) {
        Set<CaseStatus> allowedStatuses = Set.of(
                CaseStatus.PENDING,
                CaseStatus.ASSIGNED,
                CaseStatus.ACCEPTED,
                CaseStatus.SCHEDULED,
                CaseStatus.PAYMENT_PENDING,
                CaseStatus.IN_PROGRESS
        );

        if (!allowedStatuses.contains(medicalCase.getStatus())) {
            throw new BusinessException(
                    String.format("Cannot upload files to case with status: %s", medicalCase.getStatus()),
                    HttpStatus.BAD_REQUEST);
        }
    }
}