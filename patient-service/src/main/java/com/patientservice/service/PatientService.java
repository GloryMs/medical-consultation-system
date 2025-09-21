package com.patientservice.service;

import com.commonlibrary.dto.AppointmentDto;
import com.commonlibrary.dto.NotificationDto;
import com.commonlibrary.dto.PaymentDto;
import com.commonlibrary.dto.ProcessPaymentDto;
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

        // Update case status to PENDING and trigger smart assignment
        saved.setStatus(CaseStatus.PENDING);
        caseRepository.save(saved);

        // Trigger smart case assignment asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000); // Small delay to ensure transaction is committed
                System.out.println("A new Case has been added, Case#: " + saved.getId() + "\n");
                System.out.println("Smart Case Assignment started asynchronously @: " + LocalDateTime.now() + "\n");
                assignmentService.assignCaseToMultipleDoctors(saved.getId());
            } catch (Exception e) {
                System.out.println("Failed to assign case {} automatically: " +  saved.getId());
                e.fillInStackTrace();
            }
        });

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

    public List<CaseDto> getPatientCases(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        return caseRepository.findByPatientId(patient.getId()).stream().map(this::convertToCaseDto).collect(Collectors.toList());
    }

    public List<CaseDto> getCasesforDoctor(Long doctorId) {
        List<CaseAssignment> caseAssignments =  assignmentRepository.findByDoctorId( doctorId);
        List<Case> casesForDoctor = new ArrayList<>();
        List<Case> allCasses = caseRepository.findAllCases();

        for (CaseAssignment caseAssignment : caseAssignments) {
            Case newCase = allCasses.stream().
                    filter(p-> p.getId().equals(caseAssignment.getCaseEntity().getId()) ).toList().get(0);
            casesForDoctor.add(newCase);
        }

        return casesForDoctor.stream().map(this::convertToCaseDto).collect(Collectors.toList());
    }

    public List<CaseDto> getCasesPool(String specialization){
        List<CaseDto> caseDtos = new ArrayList<>();
        caseDtos = caseRepository.findCaseByRequiredSpecializationAndStatus(specialization,
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
    public void acceptAssignment(Long doctorId, Long assignmentId){
        CaseAssignment caseAssignment =  caseAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException("Assignment not found", HttpStatus.NOT_FOUND));
        if (!caseAssignment.getDoctorId().equals(doctorId)) {
            throw new BusinessException("Unauthorized access", HttpStatus.FORBIDDEN);
        }

        if (caseAssignment.getStatus() != AssignmentStatus.PENDING) {
            throw new BusinessException("Assignment cannot be accepted", HttpStatus.BAD_REQUEST);
        }

        caseAssignment.setStatus(AssignmentStatus.ACCEPTED);
        caseAssignment.setRespondedAt(LocalDateTime.now());
        caseAssignmentRepository.save(caseAssignment);

        // Update case status if this is the primary assignment
        if (caseAssignment.getPriority() == AssignmentPriority.PRIMARY) {
            Case medicalCase = caseAssignment.getCaseEntity();
            medicalCase.setStatus(ACCEPTED);
            caseRepository.save(medicalCase);
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
            throw new BusinessException("Assignment cannot be accepted", HttpStatus.BAD_REQUEST);
        }

        assignment.setStatus(AssignmentStatus.REJECTED);
        assignment.setRejectionReason(reason);
        assignment.setRespondedAt(LocalDateTime.now());
        caseAssignmentRepository.save(assignment);

        // Update doctor's case load
        /*TODO
        *  1- Call update doctor work load function in db
        *  2- Trigger case assignment algorithm*/
//        doctor.setCurrentCaseLoad(Math.max(0, doctor.getCurrentCaseLoad() - 1));
//        doctor.setRejectedCases(doctor.getRejectedCases() + 1);
//        doctorRepository.save(doctor);
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

        Long activeCasesCount = caseRepository.countByStatusIn(activeStatusList);
        Long completedCasesCount = caseRepository.countByStatus(CONSULTATION_COMPLETE);
        Long inProgressCasesCunt = caseRepository.countByStatus(IN_PROGRESS);
        Long activeSubscriptionsCount = patientRepository.countBySubscriptionStatus(SubscriptionStatus.ACTIVE);
        //Long averageCaseResolutionTime = Long.valueOf(caseRepository.calculateAverageResolutionTime());

        activeStatusList.add(CaseStatus.CLOSED);
        Long totalCasesCount = caseRepository.countByStatusIn(activeStatusList);

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

    public PatientDashboardDto getPatientDashboard(Long patientId){

        /*TODO
          below lines must be deleted, the query for the patient, and get its ID to be used to get the appointments.
         */
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));
        Long userId = patient.getUserId();

        PatientDashboardDto dto = new PatientDashboardDto();
        try{
            List<CaseStatus> statusList = new ArrayList<>();
            StatsDto stats = new StatsDto();
            statusList.add(ACCEPTED);
            statusList.add(ASSIGNED);
            statusList.add(IN_PROGRESS);
            statusList.add(SCHEDULED);
            statusList.add(CONSULTATION_COMPLETE);
            stats.setTotalCases( caseRepository.countByPatientId(patientId) );
            stats.setActiveCases( caseRepository.countByStatusInAndPatientId(statusList, patientId) );
            stats.setCompletedCases(caseRepository.countByPatientIdAndStatus(patientId ,CLOSED));
            dto.setStats(stats);
            List<Case> recentCases = caseRepository.findLastSubmittedCases(patientId, 3);
            dto.setRecentCases(recentCases.stream().map(this::convertToCaseDto).toList());
            dto.setUpcomingAppointments(getPatientAppointments(userId));
            stats.setUpcomingAppointments(dto.getUpcomingAppointments().stream().count());
            List<NotificationDto> recentNotifications = getMyNotifications(patientId);
            dto.setRecentNotifications(recentNotifications);
        }catch(Exception e){
            log.error("Failed to get patient dashboard");
            log.error(e.getMessage());
            e.printStackTrace();
        }
        return dto;
    }

    public List<NotificationDto> getMyNotifications(Long patientId){
        List<NotificationDto> dtos = new ArrayList<>();
        try{
            dtos = notificationServiceClient.getUserNotifications(patientId).getBody().getData();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return dtos;
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
}