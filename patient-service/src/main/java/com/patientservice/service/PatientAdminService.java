package com.patientservice.service;

import com.commonlibrary.dto.CaseDto;
import com.commonlibrary.dto.CaseMetricsDto;
import com.commonlibrary.entity.AssignmentPriority;
import com.commonlibrary.entity.AssignmentStatus;
import com.commonlibrary.entity.CaseStatus;
import com.commonlibrary.entity.UrgencyLevel;
import com.commonlibrary.exception.BusinessException;
import com.patientservice.dto.CaseDetailsDto;
import com.patientservice.entity.Case;
import com.patientservice.entity.CaseAssignment;
import com.patientservice.entity.Document;
import com.patientservice.entity.Patient;
import com.patientservice.repository.CaseAssignmentRepository;
import com.patientservice.repository.CaseRepository;
import com.patientservice.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for admin operations on patient cases
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PatientAdminService {

    private final CaseRepository caseRepository;
    private final CaseAssignmentRepository caseAssignmentRepository;
    private final PatientRepository patientRepository;


    /**
     * Get all cases for admin with filtering
     */
    public List<CaseDto> getAllCasesForAdmin(
            String status, String urgencyLevel, String specialization,
            Long patientId, Long doctorId, String startDate, String endDate, String searchTerm) {
        
        try {
            List<Case> cases = caseRepository.findAllCasesByIsDeletedFalse();
            
            // Apply filters
            if (status != null && !status.isEmpty()) {
                CaseStatus caseStatus = CaseStatus.valueOf(status);
                cases = cases.stream()
                        .filter(c -> c.getStatus() == caseStatus)
                        .collect(Collectors.toList());
            }
            
            if (urgencyLevel != null && !urgencyLevel.isEmpty()) {
                UrgencyLevel urgency = UrgencyLevel.valueOf(urgencyLevel);
                cases = cases.stream()
                        .filter(c -> c.getUrgencyLevel() == urgency)
                        .collect(Collectors.toList());
            }
            
            if (specialization != null && !specialization.isEmpty()) {
                cases = cases.stream()
                        .filter(c -> specialization.equals(c.getRequiredSpecialization()))
                        .collect(Collectors.toList());
            }
            
            if (patientId != null) {
                cases = cases.stream()
                        .filter(c -> c.getPatient() != null && patientId.equals(c.getPatient().getId()))
                        .collect(Collectors.toList());
            }
            
            if (doctorId != null) {
                // Filter by assigned doctor
                List<Long> caseIdsForDoctor = caseAssignmentRepository.findByCaseEntityIdIn(
                        cases.stream().map(Case::getId).collect(Collectors.toList())
                ).stream()
                .filter(assignment -> doctorId.equals(assignment.getDoctorId()))
                .filter(assignment -> assignment.getStatus() == AssignmentStatus.ACCEPTED || 
                                     assignment.getStatus() == AssignmentStatus.PENDING)
                .map(assignment -> assignment.getCaseEntity().getId())
                .collect(Collectors.toList());
                
                cases = cases.stream()
                        .filter(c -> caseIdsForDoctor.contains(c.getId()))
                        .collect(Collectors.toList());
            }
            
            if (startDate != null && !startDate.isEmpty()) {
                LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
                cases = cases.stream()
                        .filter(c -> c.getSubmittedAt() != null && !c.getSubmittedAt().isBefore(start))
                        .collect(Collectors.toList());
            }
            
            if (endDate != null && !endDate.isEmpty()) {
                LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
                cases = cases.stream()
                        .filter(c -> c.getSubmittedAt() != null && !c.getSubmittedAt().isAfter(end))
                        .collect(Collectors.toList());
            }
            
            if (searchTerm != null && !searchTerm.isEmpty()) {
                String searchLower = searchTerm.toLowerCase();
                cases = cases.stream()
                        .filter(c -> (c.getCaseTitle() != null && c.getCaseTitle().toLowerCase().contains(searchLower)) ||
                                    (c.getDescription() != null && c.getDescription().toLowerCase().contains(searchLower)) ||
                                    (c.getPatient() != null && c.getPatient().getFullName() != null && 
                                     c.getPatient().getFullName().toLowerCase().contains(searchLower)))
                        .collect(Collectors.toList());
            }
            
            // Sort by submission date (newest first)
            cases.sort((c1, c2) -> {
                if (c1.getSubmittedAt() == null) return 1;
                if (c2.getSubmittedAt() == null) return -1;
                return c2.getSubmittedAt().compareTo(c1.getSubmittedAt());
            });
            
            return cases.stream()
                    .map(this::convertToCaseDto)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Error fetching cases for admin: {}", e.getMessage(), e);
            throw new BusinessException("Failed to fetch cases", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get case assignment history
     */
    public List<Map<String, Object>> getCaseAssignmentHistory(Long caseId) {
        try {
            List<CaseAssignment> assignments = caseAssignmentRepository.findByCaseEntityId(caseId);
            
            // Sort by assigned date (newest first)
            assignments.sort((a1, a2) -> {
                if (a1.getAssignedAt() == null) return 1;
                if (a2.getAssignedAt() == null) return -1;
                return a2.getAssignedAt().compareTo(a1.getAssignedAt());
            });
            
            return assignments.stream()
                    .map(this::convertAssignmentToMap)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Error fetching assignment history: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Create case assignment (admin initiated)
     */
    public void createCaseAssignment(Map<String, Object> assignmentRequest) {
        try {
            Long caseId = getLongFromMap(assignmentRequest, "caseId");
            Long doctorId = getLongFromMap(assignmentRequest, "doctorId");
            String priorityStr = (String) assignmentRequest.get("priority");
            String assignedBy = (String) assignmentRequest.get("assignedBy");
            Long adminUserId = getLongFromMap(assignmentRequest, "adminUserId");
            String reason = (String) assignmentRequest.get("reassignmentReason");
            
            Case medicalCase = caseRepository.findById(caseId)
                    .orElseThrow(() -> new BusinessException("Case not found", HttpStatus.NOT_FOUND));
            
            AssignmentPriority priority = priorityStr != null ? 
                    AssignmentPriority.valueOf(priorityStr) : AssignmentPriority.PRIMARY;
            
            // Create assignment
            CaseAssignment assignment = CaseAssignment.builder()
                    .caseEntity(medicalCase)
                    .doctorId(doctorId)
                    .status(AssignmentStatus.PENDING)
                    .priority(priority)
                    .assignedAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(2)) // 48 hours to respond
                    .assignmentReason(reason != null ? reason : "Assigned by admin")
                    .build();
            
            caseAssignmentRepository.save(assignment);
            
            // Update case status to ASSIGNED if it was PENDING
            if (medicalCase.getStatus() == CaseStatus.PENDING || 
                medicalCase.getStatus() == CaseStatus.SUBMITTED) {
                medicalCase.setStatus(CaseStatus.ASSIGNED);
                if (medicalCase.getFirstAssignedAt() == null) {
                    medicalCase.setFirstAssignedAt(LocalDateTime.now());
                }
                medicalCase.setLastAssignedAt(LocalDateTime.now());
                medicalCase.setAssignmentAttempts(medicalCase.getAssignmentAttempts() + 1);
                caseRepository.save(medicalCase);
            }
            
            log.info("Created assignment for case {} to doctor {} by {}", 
                     caseId, doctorId, assignedBy);
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating case assignment: {}", e.getMessage(), e);
            throw new BusinessException("Failed to create assignment", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update assignment status
     */
    public void updateAssignmentStatus(Map<String, Object> updateRequest) {
        try {
            Long caseId = getLongFromMap(updateRequest, "caseId");
            Long doctorId = getLongFromMap(updateRequest, "doctorId");
            String statusStr = (String) updateRequest.get("status");
            
            AssignmentStatus newStatus = AssignmentStatus.valueOf(statusStr);
            
            List<CaseAssignment> assignments = caseAssignmentRepository.findByCaseEntityId(caseId);
            
            for (CaseAssignment assignment : assignments) {
                if (assignment.getDoctorId().equals(doctorId)) {
                    assignment.setStatus(newStatus);
                    if (newStatus == AssignmentStatus.ACCEPTED || newStatus == AssignmentStatus.REJECTED) {
                        assignment.setRespondedAt(LocalDateTime.now());
                    }
                    caseAssignmentRepository.save(assignment);
                    log.info("Updated assignment status for case {} doctor {} to {}", 
                             caseId, doctorId, newStatus);
                    break;
                }
            }
            
        } catch (Exception e) {
            log.error("Error updating assignment status: {}", e.getMessage(), e);
            throw new BusinessException("Failed to update assignment status", 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update case status (admin override)
     */
    public void updateCaseStatusAdmin(Long caseId, Map<String, Object> statusUpdate) {
        try {
            String statusStr = (String) statusUpdate.get("status");
            String notes = (String) statusUpdate.get("notes");
            
            Case medicalCase = caseRepository.findById(caseId)
                    .orElseThrow(() -> new BusinessException("Case not found", HttpStatus.NOT_FOUND));
            
            CaseStatus newStatus = CaseStatus.valueOf(statusStr);
            CaseStatus oldStatus = medicalCase.getStatus();
            
            medicalCase.setStatus(newStatus);
            medicalCase.setUpdatedAt(LocalDateTime.now());
            
            // Update relevant timestamps
            if (newStatus == CaseStatus.CLOSED && medicalCase.getClosedAt() == null) {
                medicalCase.setClosedAt(LocalDateTime.now());
            }
            
            caseRepository.save(medicalCase);
            
            log.info("Admin updated case {} status from {} to {} - Notes: {}", 
                     caseId, oldStatus, newStatus, notes);
            
        } catch (Exception e) {
            log.error("Error updating case status: {}", e.getMessage(), e);
            throw new BusinessException("Failed to update case status", 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get case metrics
     */
    public CaseMetricsDto getCaseMetrics(String startDate, String endDate) {
        try {
            LocalDateTime start = startDate != null ? 
                    LocalDate.parse(startDate).atStartOfDay() : 
                    LocalDateTime.now().minusMonths(12);
            LocalDateTime end = endDate != null ? 
                    LocalDate.parse(endDate).atTime(23, 59, 59) : 
                    LocalDateTime.now();
            
            List<Case> allCases = caseRepository.findAllCasesByIsDeletedFalse();
            
            // Filter by date range
            List<Case> filteredCases = allCases.stream()
                    .filter(c -> c.getSubmittedAt() != null)
                    .filter(c -> !c.getSubmittedAt().isBefore(start) && !c.getSubmittedAt().isAfter(end))
                    .collect(Collectors.toList());
            
            CaseMetricsDto metrics = new CaseMetricsDto();
            
            // Overall counts
            metrics.setTotalCases((long) filteredCases.size());
            metrics.setActiveCases(filteredCases.stream()
                    .filter(c -> c.getStatus() != CaseStatus.CLOSED && 
                                c.getStatus() != CaseStatus.REJECTED)
                    .count());
            metrics.setClosedCases(filteredCases.stream()
                    .filter(c -> c.getStatus() == CaseStatus.CLOSED)
                    .count());
            
            // Status breakdown
            metrics.setSubmittedCount(countByStatus(filteredCases, CaseStatus.SUBMITTED));
            metrics.setPendingCount(countByStatus(filteredCases, CaseStatus.PENDING));
            metrics.setAssignedCount(countByStatus(filteredCases, CaseStatus.ASSIGNED));
            metrics.setAcceptedCount(countByStatus(filteredCases, CaseStatus.ACCEPTED));
            metrics.setScheduledCount(countByStatus(filteredCases, CaseStatus.SCHEDULED));
            metrics.setPaymentPendingCount(countByStatus(filteredCases, CaseStatus.PAYMENT_PENDING));
            metrics.setInProgressCount(countByStatus(filteredCases, CaseStatus.IN_PROGRESS));
            metrics.setConsultationCompleteCount(countByStatus(filteredCases, CaseStatus.CONSULTATION_COMPLETE));
            metrics.setRejectedCount(countByStatus(filteredCases, CaseStatus.REJECTED));
            
            // Urgency breakdown
            metrics.setLowUrgencyCount(countByUrgency(filteredCases, UrgencyLevel.LOW));
            metrics.setMediumUrgencyCount(countByUrgency(filteredCases, UrgencyLevel.MEDIUM));
            metrics.setHighUrgencyCount(countByUrgency(filteredCases, UrgencyLevel.HIGH));
            metrics.setCriticalUrgencyCount(countByUrgency(filteredCases, UrgencyLevel.CRITICAL));
            
            // Time metrics
            metrics.setAverageAssignmentTime(calculateAverageAssignmentTime(filteredCases));
            metrics.setAverageResolutionTime(calculateAverageResolutionTime(filteredCases));
            metrics.setAverageResponseTime(calculateAverageResponseTime(filteredCases));
            
            // Assignment metrics
            metrics.setUnassignedCasesCount(filteredCases.stream()
                    .filter(c -> c.getStatus() == CaseStatus.PENDING || 
                                c.getStatus() == CaseStatus.SUBMITTED)
                    .count());
            
            // Specialization breakdown
            metrics.setCasesBySpecialization(getCasesBySpecialization(filteredCases));
            
            // Monthly trends
            metrics.setCasesPerMonth(getCasesPerMonth(filteredCases));
            
            // Status distribution
            metrics.setStatusDistribution(calculateStatusDistribution(filteredCases));
            
            return metrics;
            
        } catch (Exception e) {
            log.error("Error calculating metrics: {}", e.getMessage(), e);
            return new CaseMetricsDto();
        }
    }

    /**
     * Get unassigned cases
     */
    public List<CaseDto> getUnassignedCases(String urgencyLevel, String specialization) {
        try {
            List<Case> cases = caseRepository.findAllCasesByIsDeletedFalse().stream()
                    .filter(c -> c.getStatus() == CaseStatus.PENDING || 
                                c.getStatus() == CaseStatus.SUBMITTED)
                    .collect(Collectors.toList());
            
            if (urgencyLevel != null && !urgencyLevel.isEmpty()) {
                UrgencyLevel urgency = UrgencyLevel.valueOf(urgencyLevel);
                cases = cases.stream()
                        .filter(c -> c.getUrgencyLevel() == urgency)
                        .collect(Collectors.toList());
            }
            
            if (specialization != null && !specialization.isEmpty()) {
                cases = cases.stream()
                        .filter(c -> specialization.equals(c.getRequiredSpecialization()))
                        .collect(Collectors.toList());
            }
            
            return cases.stream()
                    .map(this::convertToCaseDto)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Error fetching unassigned cases: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get overdue cases
     */
    public List<CaseDto> getOverdueCases() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime cutoff = now.minusDays(7); // Cases submitted more than 7 days ago
            
            List<Case> cases = caseRepository.findAllCasesByIsDeletedFalse().stream()
                    .filter(c -> c.getStatus() != CaseStatus.CLOSED && 
                                c.getStatus() != CaseStatus.REJECTED)
                    .filter(c -> c.getSubmittedAt() != null && c.getSubmittedAt().isBefore(cutoff))
                    .collect(Collectors.toList());
            
            return cases.stream()
                    .map(this::convertToCaseDto)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Error fetching overdue cases: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get patient basic info
     */
    public Map<String, Object> getPatientBasicInfo(Long patientId) {
        try {
            Patient patient = patientRepository.findById(patientId)
                    .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));
            
            Map<String, Object> info = new HashMap<>();
            info.put("id", patient.getId());
            info.put("userId", patient.getUserId());
            info.put("fullName", patient.getFullName());
            info.put("email", patient.getEmail());
            info.put("phoneNumber", patient.getPhoneNumber());
            info.put("dateOfBirth", patient.getDateOfBirth());
            info.put("gender", patient.getGender());
            info.put("subscriptionStatus", patient.getSubscriptionStatus());
            
            return info;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching patient info: {}", e.getMessage(), e);
            throw new BusinessException("Failed to fetch patient info", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get case statistics by status
     */
    public Map<String, Long> getCaseStatisticsByStatus() {
        List<Case> cases = caseRepository.findAllCasesByIsDeletedFalse();
        
        Map<String, Long> stats = new HashMap<>();
        for (CaseStatus status : CaseStatus.values()) {
            stats.put(status.name(), countByStatus(cases, status));
        }
        
        return stats;
    }

    /**
     * Get case statistics by urgency
     */
    public Map<String, Long> getCaseStatisticsByUrgency() {
        List<Case> cases = caseRepository.findAllCasesByIsDeletedFalse();
        
        Map<String, Long> stats = new HashMap<>();
        for (UrgencyLevel urgency : UrgencyLevel.values()) {
            stats.put(urgency.name(), countByUrgency(cases, urgency));
        }
        
        return stats;
    }

    /**
     * Get case statistics by specialization
     */
    public Map<String, Long> getCaseStatisticsBySpecialization() {
        List<Case> cases = caseRepository.findAllCasesByIsDeletedFalse();
        return getCasesBySpecialization(cases);
    }

    // ==================== Helper Methods ====================

    private CaseDto convertToCaseDto(Case medicalCase) {
        ModelMapper modelMapper = new ModelMapper();
        CaseDto dto = modelMapper.map(medicalCase, CaseDto.class);
        dto.setPatientId(medicalCase.getPatient().getId());
        
        if (medicalCase.getPatient() != null) {
            dto.setPatientName(medicalCase.getPatient().getFullName());
        }
        
        return dto;
    }

    private Map<String, Object> convertAssignmentToMap(CaseAssignment assignment) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", assignment.getId());
        map.put("caseId", assignment.getCaseEntity().getId());
        map.put("doctorId", assignment.getDoctorId());
        map.put("status", assignment.getStatus().name());
        map.put("priority", assignment.getPriority().name());
        map.put("assignedAt", assignment.getAssignedAt());
        map.put("respondedAt", assignment.getRespondedAt());
        map.put("expiresAt", assignment.getExpiresAt());
        map.put("assignmentReason", assignment.getAssignmentReason());
        return map;
    }

    private Long getLongFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof String) return Long.parseLong((String) value);
        return null;
    }

    private long countByStatus(List<Case> cases, CaseStatus status) {
        return cases.stream().filter(c -> c.getStatus() == status).count();
    }

    private long countByUrgency(List<Case> cases, UrgencyLevel urgency) {
        return cases.stream().filter(c -> c.getUrgencyLevel() == urgency).count();
    }

    private Double calculateAverageAssignmentTime(List<Case> cases) {
        List<Long> assignmentTimes = cases.stream()
                .filter(c -> c.getSubmittedAt() != null && c.getFirstAssignedAt() != null)
                .map(c -> ChronoUnit.HOURS.between(c.getSubmittedAt(), c.getFirstAssignedAt()))
                .collect(Collectors.toList());
        
        if (assignmentTimes.isEmpty()) return 0.0;
        return assignmentTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    private Double calculateAverageResolutionTime(List<Case> cases) {
        List<Long> resolutionTimes = cases.stream()
                .filter(c -> c.getSubmittedAt() != null && c.getClosedAt() != null)
                .map(c -> ChronoUnit.DAYS.between(c.getSubmittedAt(), c.getClosedAt()))
                .collect(Collectors.toList());
        
        if (resolutionTimes.isEmpty()) return 0.0;
        return resolutionTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    private Double calculateAverageResponseTime(List<Case> cases) {
        List<Long> responseTimes = new ArrayList<>();
        
        for (Case medicalCase : cases) {
            if (medicalCase.getFirstAssignedAt() != null) {
                List<CaseAssignment> assignments = caseAssignmentRepository.findByCaseEntityId(medicalCase.getId());
                for (CaseAssignment assignment : assignments) {
                    if (assignment.getRespondedAt() != null && assignment.getAssignedAt() != null) {
                        long hours = ChronoUnit.HOURS.between(assignment.getAssignedAt(), assignment.getRespondedAt());
                        responseTimes.add(hours);
                    }
                }
            }
        }
        
        if (responseTimes.isEmpty()) return 0.0;
        return responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    private Map<String, Long> getCasesBySpecialization(List<Case> cases) {
        return cases.stream()
                .filter(c -> c.getRequiredSpecialization() != null)
                .collect(Collectors.groupingBy(
                        Case::getRequiredSpecialization,
                        Collectors.counting()
                ));
    }

    private Map<String, Long> getCasesPerMonth(List<Case> cases) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        
        return cases.stream()
                .filter(c -> c.getSubmittedAt() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getSubmittedAt().format(formatter),
                        Collectors.counting()
                ));
    }

    private Map<String, Double> calculateStatusDistribution(List<Case> cases) {
        long total = cases.size();
        if (total == 0) return Collections.emptyMap();
        
        Map<String, Double> distribution = new HashMap<>();
        for (CaseStatus status : CaseStatus.values()) {
            long count = countByStatus(cases, status);
            double percentage = (count * 100.0) / total;
            distribution.put(status.name(), percentage);
        }
        
        return distribution;
    }

    public CaseDto getCaseDetails( Long caseId) {

        //TODO make sure that admin access (by getting admin userId)
        ModelMapper mapper = new ModelMapper();

        Case medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new BusinessException("Case not found", HttpStatus.NOT_FOUND));
        CaseDto details = new CaseDto();
        try{
            mapper.map(medicalCase, details);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException("Error while mapping case date to DTO", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return details;
    }
}