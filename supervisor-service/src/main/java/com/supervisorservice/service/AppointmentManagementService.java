package com.supervisorservice.service;

import com.commonlibrary.dto.*;
import com.commonlibrary.dto.ApiResponse;
import com.commonlibrary.entity.AppointmentStatus;
import com.commonlibrary.entity.RescheduleStatus;
import com.commonlibrary.exception.BusinessException;
import com.supervisorservice.dto.*;
import com.supervisorservice.entity.MedicalSupervisor;
import com.supervisorservice.entity.SupervisorPatientAssignment;
import com.supervisorservice.feign.DoctorServiceClient;
import com.supervisorservice.feign.PatientServiceClient;
import com.supervisorservice.kafka.SupervisorKafkaConsumer;
import com.supervisorservice.kafka.SupervisorKafkaProducer;
import com.supervisorservice.repository.MedicalSupervisorRepository;
import com.supervisorservice.repository.SupervisorPatientAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing appointments on behalf of assigned patients
 * Handles appointment viewing, acceptance, and reschedule requests
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentManagementService {

    private final MedicalSupervisorRepository supervisorRepository;
    private final SupervisorPatientAssignmentRepository assignmentRepository;
    private final DoctorServiceClient doctorServiceClient;
    private final PatientServiceClient patientServiceClient;
    private final SupervisorValidationService validationService;
    private final SupervisorKafkaProducer eventProducer;

    /**
     * Get all appointments for supervisor's assigned patients
     * @param userId Supervisor's user ID
     * @param filterDto Optional filters
     * @return List of appointments
     */
    public List<SupervisorAppointmentDto> getAppointments(Long userId, AppointmentFilterDto filterDto) {
        log.info("Getting appointments for supervisor userId: {} with filters: {}", userId, filterDto);

        MedicalSupervisor supervisor = getSupervisorByUserId(userId);
        validationService.validateSupervisorVerified(supervisor);

        // Get all assigned patient IDs
        List<Long> assignedPatientIds = getAssignedPatientIds(supervisor.getId());
        
        if (assignedPatientIds.isEmpty()) {
            log.info("No patients assigned to supervisor {}", supervisor.getId());
            return Collections.emptyList();
        }

        // If specific patient filter is provided, validate assignment
        if (filterDto != null && filterDto.getPatientId() != null) {
            validatePatientAssignment(supervisor.getId(), filterDto.getPatientId());
            assignedPatientIds = Collections.singletonList(filterDto.getPatientId());
        }

        // Fetch appointments for all assigned patients
        List<SupervisorAppointmentDto> allAppointments = new ArrayList<>();
        
        for (Long patientId : assignedPatientIds) {
            try {
                List<AppointmentDto> patientAppointments = fetchPatientAppointments(patientId, filterDto);
                
                // Convert and enrich appointments
                for (AppointmentDto appointment : patientAppointments) {
                    SupervisorAppointmentDto enrichedAppointment = enrichAppointment(appointment, supervisor.getId());
                    allAppointments.add(enrichedAppointment);
                }
            } catch (Exception e) {
                log.warn("Error fetching appointments for patient {}: {}", patientId, e.getMessage());
            }
        }

        // Apply additional filters
        allAppointments = applyFilters(allAppointments, filterDto);

        // Sort appointments
        sortAppointments(allAppointments, filterDto);

        log.info("Found {} appointments for supervisor {}", allAppointments.size(), supervisor.getId());
        return allAppointments;
    }

    /**
     * Get appointment details by ID
     * @param userId Supervisor's user ID
     * @param appointmentId Appointment ID
     * @return Appointment details
     */
    public SupervisorAppointmentDto getAppointmentDetails(Long userId, Long appointmentId) {
        log.info("Getting appointment {} details for supervisor userId: {}", appointmentId, userId);

        MedicalSupervisor supervisor = getSupervisorByUserId(userId);
        validationService.validateSupervisorVerified(supervisor);

        // Fetch appointment from doctor-service
        AppointmentDto appointment = fetchAppointmentById(appointmentId);
        
        // Validate patient is assigned to this supervisor
        validatePatientAssignment(supervisor.getId(), appointment.getPatientId());

        return enrichAppointment(appointment, supervisor.getId());
    }

    /**
     * Get upcoming appointments for supervisor's patients
     * @param userId Supervisor's user ID
     * @return List of upcoming appointments
     */
    public List<SupervisorAppointmentDto> getUpcomingAppointments(Long userId) {
        AppointmentFilterDto filter = AppointmentFilterDto.builder()
                .upcomingOnly(true)
                .build();
        return getAppointments(userId, filter);
    }

    /**
     * Get appointments for a specific patient
     * @param userId Supervisor's user ID
     * @param patientId Patient ID
     * @return List of patient's appointments
     */
    public List<SupervisorAppointmentDto> getPatientAppointments(Long userId, Long patientId) {
        AppointmentFilterDto filter = AppointmentFilterDto.builder()
                .patientId(patientId)
                .build();
        return getAppointments(userId, filter);
    }

    /**
     * Get appointments for a specific case
     * @param userId Supervisor's user ID
     * @param caseId Case ID
     * @return List of case appointments
     */
    public List<SupervisorAppointmentDto> getCaseAppointments(Long userId, Long caseId) {
        log.info("Getting appointments for case {} by supervisor userId: {}", caseId, userId);

        MedicalSupervisor supervisor = getSupervisorByUserId(userId);
        validationService.validateSupervisorVerified(supervisor);

        // Get case to verify patient assignment
        CaseDto caseDto = fetchCaseById(caseId);
        validatePatientAssignment(supervisor.getId(), caseDto.getPatientId());

        // Fetch appointments for the case
        try {
            ApiResponse<List<AppointmentDto>> response = doctorServiceClient
                    .getAppointmentsByCaseId(caseId).getBody();
            
            if (response != null && response.getData() != null) {
                return response.getData().stream()
                        .map(apt -> enrichAppointment(apt, supervisor.getId()))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Error fetching appointments for case {}: {}", caseId, e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     * Accept appointment on behalf of patient
     * @param userId Supervisor's user ID
     * @param dto Accept appointment request
     */
    @Transactional
    public void acceptAppointment(Long userId, AcceptAppointmentDto dto) {
        log.info("Accepting appointment for case {} patient {} by supervisor userId: {}", 
                dto.getCaseId(), dto.getPatientId(), userId);

        MedicalSupervisor supervisor = getSupervisorByUserId(userId);
        validationService.validateSupervisorVerified(supervisor);
        validatePatientAssignment(supervisor.getId(), dto.getPatientId());

        try {
            // Call patient-service to accept appointment
            patientServiceClient.acceptAppointmentBySupervisor(
                    dto.getCaseId(), 
                    dto.getPatientId(), 
                    supervisor.getId());

            // Send Kafka event
//            eventProducer.sendAppointmentAcceptedEvent(
//                    supervisor.getId(),
//                    dto.getPatientId(),
//                    dto.getCaseId(),
//                    dto.getNotes()
//            );

            log.info("Appointment accepted successfully for case {}", dto.getCaseId());
        } catch (Exception e) {
            log.error("Error accepting appointment for case {}: {}", dto.getCaseId(), e.getMessage());
            throw new BusinessException("Failed to accept appointment: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Create reschedule request on behalf of patient
     * @param userId Supervisor's user ID
     * @param dto Reschedule request data
     * @return Created reschedule request
     */
    @Transactional
    public RescheduleRequestResponseDto createRescheduleRequest(Long userId, SupervisorRescheduleRequestDto dto) {
        log.info("Creating reschedule request for appointment {} by supervisor userId: {}", 
                dto.getAppointmentId(), userId);

        MedicalSupervisor supervisor = getSupervisorByUserId(userId);
        validationService.validateSupervisorVerified(supervisor);
        validatePatientAssignment(supervisor.getId(), dto.getPatientId());

        // ====================================================================
        // VALIDATE PREFERRED TIMES
        // ====================================================================
        LocalDateTime now = LocalDateTime.now();
        for (String preferredTime : dto.getPreferredTimes()) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(preferredTime);

                if (dateTime.isBefore(now)) {
                    log.warn("Preferred time is in the past: {}", preferredTime);
                    throw new BusinessException(
                            "Preferred times must be in the future",
                            HttpStatus.BAD_REQUEST
                    );
                }

                log.debug("Preferred time validated: {}", preferredTime);
            } catch (Exception e) {
                log.error("Invalid date format: {}", preferredTime);
                throw new BusinessException(
                        "Invalid date format. Use ISO 8601 format (yyyy-MM-ddThh:mm:ss)",
                        HttpStatus.BAD_REQUEST
                );
            }
        }
        log.debug("All preferred times validated");

        try {
            // Create reschedule request DTO for patient-service
            RescheduleRequestDto requestDto = RescheduleRequestDto.builder()
                    .appointmentId(dto.getAppointmentId())
                    .caseId(dto.getCaseId())
                    .patientId(dto.getPatientId())
                    .preferredTimes(dto.getPreferredTimes())
                    .reason(dto.getReason())
                    .build();

            ApiResponse<RescheduleRequestResponseDto> response = patientServiceClient
                    .createRescheduleRequestBySupervisor(requestDto, supervisor.getId())
                    .getBody();

            if (response != null && response.getData() != null) {

                //TODO check if there is a need to:
                // Send Kafka event
//                eventProducer.sendRescheduleRequestCreatedEvent(
//                        supervisor.getId(),
//                        dto.getPatientId(),
//                        dto.getCaseId(),
//                        dto.getAppointmentId(),
//                        dto.getRequestedTime(),
//                        dto.getReason()
//                );

                log.info("Reschedule request created successfully");
                return response.getData();
            }

            throw new BusinessException("Failed to create reschedule request", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating reschedule request: {}", e.getMessage());
            throw new BusinessException("Failed to create reschedule request: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get reschedule requests for supervisor's patients
     * @param userId Supervisor's user ID
     * @param patientId Optional patient ID filter
     * @return List of reschedule requests
     */
    public List<RescheduleRequestResponseDto> getRescheduleRequests(Long userId, Long patientId) {
        log.info("Getting reschedule requests for supervisor userId: {}", userId);

        MedicalSupervisor supervisor = getSupervisorByUserId(userId);
        validationService.validateSupervisorVerified(supervisor);

        List<Long> patientIds;
        if (patientId != null) {
            validatePatientAssignment(supervisor.getId(), patientId);
            patientIds = Collections.singletonList(patientId);
        } else {
            patientIds = getAssignedPatientIds(supervisor.getId());
        }

        List<RescheduleRequestResponseDto> allRequests = new ArrayList<>();

        for (Long pid : patientIds) {
            try {
                ApiResponse<List<RescheduleRequestResponseDto>> response = patientServiceClient
                        .getRescheduleRequestsByPatientId(pid).getBody();
                
                if (response != null && response.getData() != null) {
                    allRequests.addAll(response.getData());
                }
            } catch (Exception e) {
                log.warn("Error fetching reschedule requests for patient {}: {}", pid, e.getMessage());
            }
        }

        // Sort by created date descending
        allRequests.sort((a, b) -> {
            if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        return allRequests;
    }

    /**
     * Get appointment summary statistics
     * @param userId Supervisor's user ID
     * @return Appointment summary
     */
    public AppointmentSummaryDto getAppointmentSummary(Long userId) {
        log.info("Getting appointment summary for supervisor userId: {}", userId);

        MedicalSupervisor supervisor = getSupervisorByUserId(userId);
        validationService.validateSupervisorVerified(supervisor);

        List<SupervisorAppointmentDto> allAppointments = getAppointments(userId, null);

        Map<String, Long> byStatus = allAppointments.stream()
                .filter(a -> a.getStatus() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getStatus().name(),
                        Collectors.counting()
                ));

        Map<String, Long> byPatient = allAppointments.stream()
                .filter(a -> a.getPatientName() != null)
                .collect(Collectors.groupingBy(
                        SupervisorAppointmentDto::getPatientName,
                        Collectors.counting()
                ));

        LocalDateTime now = LocalDateTime.now();
        long upcoming = allAppointments.stream()
                .filter(a -> a.getScheduledTime() != null && a.getScheduledTime().isAfter(now))
                .filter(a -> a.getStatus() == AppointmentStatus.SCHEDULED 
                        || a.getStatus() == AppointmentStatus.CONFIRMED
                        || a.getStatus() == AppointmentStatus.RESCHEDULED)
                .count();

        long completed = byStatus.getOrDefault(AppointmentStatus.COMPLETED.name(), 0L);
        long cancelled = byStatus.getOrDefault(AppointmentStatus.CANCELLED.name(), 0L);
        long rescheduled = byStatus.getOrDefault(AppointmentStatus.RESCHEDULED.name(), 0L);

        // Get pending reschedule requests count
        List<RescheduleRequestResponseDto> rescheduleRequests = getRescheduleRequests(userId, null);
        long pendingReschedule = rescheduleRequests.stream()
                .filter(r -> Objects.equals(r.getStatus(),
                        String.valueOf(RescheduleStatus.PENDING)))
                .count();

        return AppointmentSummaryDto.builder()
                .totalAppointments((long) allAppointments.size())
                .upcomingAppointments(upcoming)
                .completedAppointments(completed)
                .cancelledAppointments(cancelled)
                .rescheduledAppointments(rescheduled)
                .pendingRescheduleRequests(pendingReschedule)
                .appointmentsByStatus(byStatus)
                .appointmentsByPatient(byPatient)
                .build();
    }

    // ==================== Private Helper Methods ====================

    private MedicalSupervisor getSupervisorByUserId(Long userId) {
        return supervisorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Supervisor not found", HttpStatus.NOT_FOUND));
    }

    private List<Long> getAssignedPatientIds(Long supervisorId) {
        return assignmentRepository.findBySupervisorIdAndAssignmentStatus(
                        supervisorId, 
                        com.commonlibrary.entity.SupervisorAssignmentStatus.ACTIVE)
                .stream()
                .map(SupervisorPatientAssignment::getPatientId)
                .collect(Collectors.toList());
    }

    private void validatePatientAssignment(Long supervisorId, Long patientId) {
        boolean isAssigned = assignmentRepository.existsBySupervisorIdAndPatientIdAndAssignmentStatus(
                supervisorId, 
                patientId, 
                com.commonlibrary.entity.SupervisorAssignmentStatus.ACTIVE);
        
        if (!isAssigned) {
            throw new BusinessException(
                    "Patient " + patientId + " is not assigned to this supervisor", 
                    HttpStatus.FORBIDDEN);
        }
    }

    private List<AppointmentDto> fetchPatientAppointments(Long patientId, AppointmentFilterDto filter) {
        try {
            ApiResponse<List<AppointmentDto>> response;
            
            if (filter != null && Boolean.TRUE.equals(filter.getUpcomingOnly())) {
                response = doctorServiceClient.getPatientUpcomingAppointments(patientId).getBody();
            } else {
                response = doctorServiceClient.getPatientAppointments(patientId).getBody();
            }

            if (response != null && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception e) {
            log.warn("Error fetching appointments for patient {}: {}", patientId, e.getMessage());
        }
        return Collections.emptyList();
    }

    private AppointmentDto fetchAppointmentById(Long appointmentId) {
        try {
            ApiResponse<AppointmentDto> response = doctorServiceClient
                    .getAppointmentById(appointmentId).getBody();
            
            if (response != null && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception e) {
            log.error("Error fetching appointment {}: {}", appointmentId, e.getMessage());
        }
        throw new BusinessException("Appointment not found", HttpStatus.NOT_FOUND);
    }

    private CaseDto fetchCaseById(Long caseId) {
        try {
            ApiResponse<CaseDto> response = patientServiceClient.getCaseById(caseId).getBody();
            if (response != null && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception e) {
            log.error("Error fetching case {}: {}", caseId, e.getMessage());
        }
        throw new BusinessException("Case not found", HttpStatus.NOT_FOUND);
    }

    private SupervisorAppointmentDto enrichAppointment(AppointmentDto appointment, Long supervisorId) {
        SupervisorAppointmentDto enriched = SupervisorAppointmentDto.fromAppointmentDto(appointment);
        enriched.setSupervisorId(supervisorId);
        enriched.setIsSupervisorManaged(true);

        // Fetch case details for case title
        try {
            CaseDto caseDto = fetchCaseById(appointment.getCaseId());
            enriched.setCaseTitle(caseDto.getCaseTitle());
            if (enriched.getConsultationFee() == null) {
                enriched.setConsultationFee(caseDto.getConsultationFee());
            }
        } catch (Exception e) {
            log.debug("Could not fetch case details for appointment {}", appointment.getId());
        }

        // Check for pending reschedule requests
        try {
            ApiResponse<List<RescheduleRequestResponseDto>> response = patientServiceClient
                    .getRescheduleRequestsByCaseId(appointment.getCaseId()).getBody();
            
            if (response != null && response.getData() != null) {
                Optional<RescheduleRequestResponseDto> pendingRequest = response.getData().stream()
                        .filter(r -> Objects.equals(r.getStatus(),
                                RescheduleStatus.PENDING.toString()))
                        .findFirst();
                
                enriched.setHasPendingRescheduleRequest(pendingRequest.isPresent());
                pendingRequest.ifPresent(r -> enriched.setRescheduleRequestId(r.getId()));
            }
        } catch (Exception e) {
            log.debug("Could not fetch reschedule requests for case {}", appointment.getCaseId());
        }

        return enriched;
    }

    private List<SupervisorAppointmentDto> applyFilters(
            List<SupervisorAppointmentDto> appointments, 
            AppointmentFilterDto filter) {
        
        if (filter == null) {
            return appointments;
        }

        return appointments.stream()
                .filter(apt -> {
                    // Filter by status
                    if (filter.getStatus() != null && apt.getStatus() != filter.getStatus()) {
                        return false;
                    }
                    
                    // Filter by case ID
                    if (filter.getCaseId() != null && !filter.getCaseId().equals(apt.getCaseId())) {
                        return false;
                    }
                    
                    // Filter by date
                    if (filter.getDate() != null && apt.getScheduledTime() != null) {
                        LocalDate appointmentDate = apt.getScheduledTime().toLocalDate();
                        if (!appointmentDate.equals(filter.getDate())) {
                            return false;
                        }
                    }
                    
                    // Filter by date range
                    if (filter.getStartDate() != null && apt.getScheduledTime() != null) {
                        LocalDateTime startDateTime = filter.getStartDate().atStartOfDay();
                        if (apt.getScheduledTime().isBefore(startDateTime)) {
                            return false;
                        }
                    }
                    
                    if (filter.getEndDate() != null && apt.getScheduledTime() != null) {
                        LocalDateTime endDateTime = filter.getEndDate().atTime(LocalTime.MAX);
                        if (apt.getScheduledTime().isAfter(endDateTime)) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
    }

    private void sortAppointments(List<SupervisorAppointmentDto> appointments, AppointmentFilterDto filter) {
        String sortBy = filter != null && filter.getSortBy() != null ? filter.getSortBy() : "scheduledTime";
        String sortOrder = filter != null && filter.getSortOrder() != null ? filter.getSortOrder() : "asc";

        Comparator<SupervisorAppointmentDto> comparator;

        switch (sortBy.toLowerCase()) {
            case "patientname":
                comparator = Comparator.comparing(
                        SupervisorAppointmentDto::getPatientName,
                        Comparator.nullsLast(String::compareToIgnoreCase));
                break;
            case "status":
                comparator = Comparator.comparing(
                        a -> a.getStatus() != null ? a.getStatus().name() : "",
                        Comparator.nullsLast(String::compareTo));
                break;
            case "scheduledtime":
            default:
                comparator = Comparator.comparing(
                        SupervisorAppointmentDto::getScheduledTime,
                        Comparator.nullsLast(LocalDateTime::compareTo));
                break;
        }

        if ("desc".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed();
        }

        appointments.sort(comparator);
    }
}