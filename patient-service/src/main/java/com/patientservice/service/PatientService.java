package com.patientservice.service;

import com.commonlibrary.exception.BusinessException;
import com.patientservice.dto.*;
import com.patientservice.entity.*;
import com.patientservice.feign.PaymentServiceClient;
import com.patientservice.feign.NotificationServiceClient;
import com.patientservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final CaseRepository caseRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final DocumentRepository documentRepository;
    private final RescheduleRequestRepository rescheduleRequestRepository;
    private final PaymentServiceClient paymentServiceClient;
    private final NotificationServiceClient notificationServiceClient;

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

        dto.setId(saved.getId());
        dto.setAmount(amount);
        return dto;
    }

    @Transactional
    public Case createCase(Long userId, CreateCaseDto dto) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        // Check subscription status
        if (patient.getSubscriptionStatus() != SubscriptionStatus.ACTIVE) {
            throw new BusinessException("Active subscription required to submit cases", HttpStatus.FORBIDDEN);
        }

        if (patient.getAccountLocked()) {
            throw new BusinessException("Account is locked. Please complete subscription payment", HttpStatus.FORBIDDEN);
        }

        Case medicalCase = Case.builder()
                .patient(patient)
                .caseTitle(dto.getCaseTitle())
                .description(dto.getDescription())
                .category(dto.getCategory())
                .subCategory(dto.getSubCategory())
                .status(CaseStatus.SUBMITTED)
                .urgencyLevel(dto.getUrgencyLevel())
                .paymentStatus(PaymentStatus.PENDING)
                .rejectionCount(0)
                .build();

        Case saved = caseRepository.save(medicalCase);

        // Update patient's case count
        patient.setCasesSubmitted(patient.getCasesSubmitted() + 1);
        patientRepository.save(patient);

        // Move to PENDING status for doctor assignment
        saved.setStatus(CaseStatus.PENDING);
        return caseRepository.save(saved);
    }

    public List<Case> getPatientCases(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        return caseRepository.findByPatientId(patient.getId());
    }


    public List<CaseDto> getCasesforDoctor(Long doctorId) {
        return caseRepository.findByAssignedDoctorId(doctorId).stream().
                map(this::convertToCaseDto).collect(Collectors.toList());
    }

    public CaseDto convertToCaseDto(Case newCase){
        CaseDto caseDto = new CaseDto();
        caseDto.setId(newCase.getId());
        caseDto.setCaseTitle(newCase.getCaseTitle());
        caseDto.setDescription(newCase.getDescription());
        caseDto.setCategory(newCase.getCategory());
        caseDto.setCreatedAt(newCase.getCreatedAt());
        caseDto.setUrgencyLevel(newCase.getUrgencyLevel().toString());
        caseDto.setStatus(newCase.getStatus().toString());
        caseDto.setPatientId(newCase.getPatient().getId());
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

    @Transactional
    public void payConsultationFee(Long userId, Long caseId, BigDecimal amount) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        Case medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new BusinessException("Case not found", HttpStatus.NOT_FOUND));

        if (!medicalCase.getPatient().getId().equals(patient.getId())) {
            throw new BusinessException("Unauthorized access to case", HttpStatus.FORBIDDEN);
        }

        if (medicalCase.getStatus() != CaseStatus.PAYMENT_PENDING) {
            throw new BusinessException("Payment not required at this stage", HttpStatus.BAD_REQUEST);
        }

        // Simulate payment processing
        medicalCase.setConsultationFee(amount);
        medicalCase.setPaymentStatus(PaymentStatus.COMPLETED);
        medicalCase.setPaymentCompletedAt(LocalDateTime.now());
        medicalCase.setStatus(CaseStatus.IN_PROGRESS);

        caseRepository.save(medicalCase);
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
        status.setStatus(patient.getSubscriptionStatus());
        status.setExpiryDate(patient.getSubscriptionExpiry());
        status.setIsActive(patient.getSubscriptionStatus() == SubscriptionStatus.ACTIVE);

        if (activeSubscription != null) {
            status.setPlanType(activeSubscription.getPlanType());
            status.setAmount(activeSubscription.getAmount());
            status.setAutoRenew(activeSubscription.getAutoRenew());
            status.setPaymentMethod(activeSubscription.getPaymentMethod());
        }

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
        details.setCategory(medicalCase.getCategory());
        details.setSubCategory(medicalCase.getSubCategory());
        details.setStatus(medicalCase.getStatus());
        details.setUrgencyLevel(medicalCase.getUrgencyLevel());
        details.setAssignedDoctorId(medicalCase.getAssignedDoctorId());
        details.setConsultationFee(medicalCase.getConsultationFee());
        details.setPaymentStatus(medicalCase.getPaymentStatus());
        details.setCreatedAt(medicalCase.getCreatedAt());
        details.setAcceptedAt(medicalCase.getAcceptedAt());
        details.setScheduledAt(medicalCase.getScheduledAt());
        details.setPaymentCompletedAt(medicalCase.getPaymentCompletedAt());
        details.setClosedAt(medicalCase.getClosedAt());
        details.setRejectionReason(medicalCase.getRejectionReason());

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

        // Notify doctor
        notificationServiceClient.sendNotification(
                patient.getUserId(),
                medicalCase.getAssignedDoctorId(),
                "Reschedule Request",
                "Patient requested to reschedule appointment for case: " + medicalCase.getCaseTitle()
        );
    }

    // 5. Get Payment History Implementation
    public List<PaymentHistoryDto> getPaymentHistory(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        List<PaymentHistoryDto> paymentHistoryDtoList = new ArrayList<>();
        try{
            paymentHistoryDtoList = paymentServiceClient.getPatientPaymentHistory(patient.getId());
        }catch(Exception e) {
            paymentHistoryDtoList = null;
            e.printStackTrace();
        }

        return paymentHistoryDtoList;
    }

    // 6. Update Case Status Implementation (For internal use by other services)
    @Transactional
    public void updateCaseStatus(Long caseId, String status, Long doctorId) {
        Case medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new BusinessException("Case not found", HttpStatus.NOT_FOUND));

        CaseStatus newStatus = CaseStatus.valueOf(status);
        medicalCase.setStatus(newStatus);

        if (newStatus == CaseStatus.ACCEPTED) {
            medicalCase.setAcceptedAt(LocalDateTime.now());
            medicalCase.setAssignedDoctorId(doctorId);
        } else if (newStatus == CaseStatus.SCHEDULED) {
            medicalCase.setScheduledAt(LocalDateTime.now());
        } else if (newStatus == CaseStatus.IN_PROGRESS) {
            medicalCase.setPaymentCompletedAt(LocalDateTime.now());
        } else if (newStatus == CaseStatus.CONSULTATION_COMPLETE || newStatus == CaseStatus.CLOSED) {
            medicalCase.setClosedAt(LocalDateTime.now());
        }

        caseRepository.save(medicalCase);
    }
}