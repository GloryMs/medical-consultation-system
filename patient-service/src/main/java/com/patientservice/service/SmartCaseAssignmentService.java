// Fixed SmartCaseAssignmentService.java with correct config service integration
package com.patientservice.service;

import com.commonlibrary.dto.*;
import com.commonlibrary.entity.*;
import com.commonlibrary.exception.BusinessException;
import com.patientservice.dto.*;
import com.patientservice.entity.*;
import com.patientservice.feign.DoctorServiceClient;
import com.patientservice.feign.NotificationServiceClient;
import com.patientservice.kafka.PatientEventProducer;
import com.patientservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SmartCaseAssignmentService {

    private final DoctorServiceClient doctorServiceClient;
    private final CaseRepository caseRepository;
    private final CaseAssignmentRepository caseAssignmentRepository;
    private final MedicalConfigurationService configService;
    private final NotificationServiceClient notificationService;
    private final PatientEventProducer patientEventProducer;

    // Scoring weights for matching algorithm
    private static final double PRIMARY_SPECIALIZATION_WEIGHT = 0.30;
    private static final double WORKLOAD_AVAILABILITY_WEIGHT = 0.25;
    private static final double DISEASE_EXPERTISE_WEIGHT = 0.20;
    private static final double SYMPTOM_EXPERTISE_WEIGHT = 0.10;
    private static final double EXPERIENCE_WEIGHT = 0.08;
    private static final double PERFORMANCE_WEIGHT = 0.05;
    private static final double CASE_PREFERENCE_WEIGHT = 0.02;

    @Value("${case.assignment.minimum-score-threshold:35.0}")
    private Double minimumScoreThreshold;

    @Value("${case.assignment.workload-penalty-threshold:80.0}")
    private Double workloadPenaltyThreshold;

    @Value("${case.assignment.emergency-override-enabled:true}")
    private Boolean emergencyOverrideEnabled;

    /**
     * Main method to assign a case to multiple doctors using workload-aware algorithm
     */
    public List<CaseAssignment> assignCaseToMultipleDoctors(Long caseId) {
        log.info("Starting smart case assignment for case ID: {}", caseId);

        Case medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new BusinessException("Case not found", HttpStatus.NOT_FOUND));

        // Validate case can be assigned
        validateCaseForAssignment(medicalCase);

        // Get eligible doctors with workload information
        List<DoctorCapacityDto> eligibleDoctorsWithCapacity = findEligibleDoctorsWithWorkload(medicalCase);

        if (eligibleDoctorsWithCapacity.isEmpty()) {
            // Try emergency assignment if enabled
            if (emergencyOverrideEnabled && isEmergencyCase(medicalCase)) {
                eligibleDoctorsWithCapacity = findEmergencyEligibleDoctors(medicalCase);
            }

            if (eligibleDoctorsWithCapacity.isEmpty()) {
                throw new BusinessException("No eligible doctors found for this case", HttpStatus.NOT_FOUND);
            }
        }

        // Calculate workload-aware matching scores
        List<DoctorMatchingResultDto> matchingResults = calculateWorkloadAwareMatchingScores(medicalCase, eligibleDoctorsWithCapacity);

        // Sort by score and select top doctors
        List<DoctorMatchingResultDto> selectedDoctors = selectBestDoctorsWithWorkloadBalance(medicalCase, matchingResults);

        // Create assignments with workload updates
        List<CaseAssignment> assignments = createCaseAssignmentsWithWorkloadUpdate(medicalCase, selectedDoctors);

        // Update case status and metadata
        updateCaseAfterAssignment(medicalCase, assignments);

        // Send notifications
        sendAssignmentNotifications(medicalCase, assignments);

        log.info("Successfully assigned case {} to {} doctors with workload consideration", caseId, assignments.size());
        return assignments;
    }

    /**
     * Find eligible doctors with their current workload information
     */
    private List<DoctorCapacityDto> findEligibleDoctorsWithWorkload(Case medicalCase) {
        try {
            // Get doctors by specialization with workload info
            List<String> requiredSpecializations = new ArrayList<>();
            requiredSpecializations.add(medicalCase.getRequiredSpecialization());
            if (medicalCase.getSecondarySpecializations() != null) {
                requiredSpecializations.addAll(medicalCase.getSecondarySpecializations());
            }

            List<DoctorCapacityDto> eligibleDoctors = new ArrayList<>();

            for (String specialization : requiredSpecializations) {
                var response = doctorServiceClient.getAvailableDoctorsBySpecializationWithCapacity(
                        specialization, medicalCase.getMaxDoctorsAllowed() * 2
                );

                if (response != null && response.getBody() != null && response.getBody().getData() != null) {
                    eligibleDoctors.addAll(response.getBody().getData());
                }
            }

            // Remove duplicates and filter by workload capacity
            Map<Long, DoctorCapacityDto> uniqueDoctors = new HashMap<>();
            for (DoctorCapacityDto doctor : eligibleDoctors) {
                if (!uniqueDoctors.containsKey(doctor.getDoctorId())) {
                    uniqueDoctors.put(doctor.getDoctorId(), doctor);
                }
            }

            return uniqueDoctors.values().stream()
                    .filter(doctor -> isDoctorEligibleForCase(medicalCase, doctor))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error finding eligible doctors with workload: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Find doctors for emergency cases (can override normal workload limits)
     */
    private List<DoctorCapacityDto> findEmergencyEligibleDoctors(Case medicalCase) {
        try {
            log.info("Searching for emergency eligible doctors for critical case {}", medicalCase.getId());

            var response = doctorServiceClient.getEmergencyAvailableDoctors(
                    medicalCase.getRequiredSpecialization(),
                    medicalCase.getMaxDoctorsAllowed() * 2
            );

            if (response != null && response.getBody() != null && response.getBody().getData() != null) {
                return response.getBody().getData().stream()
                        .filter(doctor -> doctor.getIsAvailable())
                        .collect(Collectors.toList());
            }

            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error finding emergency eligible doctors: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Calculate workload-aware matching scores
     */
    private List<DoctorMatchingResultDto> calculateWorkloadAwareMatchingScores(
            Case medicalCase, List<DoctorCapacityDto> eligibleDoctors) {

        return eligibleDoctors.parallelStream()
                .map(doctor -> calculateWorkloadAwareMatchingScore(medicalCase, doctor))
                .filter(result -> result.getTotalScore() >= minimumScoreThreshold)
                .collect(Collectors.toList());
    }

    /**
     * Calculate comprehensive workload-aware matching score
     */
    private DoctorMatchingResultDto calculateWorkloadAwareMatchingScore(Case medicalCase, DoctorCapacityDto doctor) {
        Map<String, Double> scoreBreakdown = new HashMap<>();
        double totalScore = 0.0;

        // 1. Primary Specialization Score (30%)
        double specializationScore = calculateSpecializationScore(medicalCase, doctor);
        scoreBreakdown.put("specialization", specializationScore);
        totalScore += specializationScore * PRIMARY_SPECIALIZATION_WEIGHT;

        // 2. Workload & Availability Score (25%)
        double workloadScore = calculateWorkloadAvailabilityScore(medicalCase, doctor);
        scoreBreakdown.put("workload_availability", workloadScore);
        totalScore += workloadScore * WORKLOAD_AVAILABILITY_WEIGHT;

        // 3. Disease Expertise Score (20%) - FIXED: Using existing config service methods
        double diseaseScore = calculateDiseaseExpertiseScoreFixed(medicalCase, doctor);
        scoreBreakdown.put("disease_expertise", diseaseScore);
        totalScore += diseaseScore * DISEASE_EXPERTISE_WEIGHT;

        // 4. Symptom Expertise Score (10%) - FIXED: Simplified approach
        double symptomScore = calculateSymptomExpertiseScoreFixed(medicalCase, doctor);
        scoreBreakdown.put("symptom_expertise", symptomScore);
        totalScore += symptomScore * SYMPTOM_EXPERTISE_WEIGHT;

        // 5. Experience Score (8%)
        double experienceScore = calculateExperienceScore(doctor);
        scoreBreakdown.put("experience", experienceScore);
        totalScore += experienceScore * EXPERIENCE_WEIGHT;

        // 6. Performance Score (5%)
        double performanceScore = calculatePerformanceScore(doctor);
        scoreBreakdown.put("performance", performanceScore);
        totalScore += performanceScore * PERFORMANCE_WEIGHT;

        // 7. Case Preference Score (2%)
        double preferenceScore = calculateCasePreferenceScore(medicalCase, doctor);
        scoreBreakdown.put("case_preference", preferenceScore);
        totalScore += preferenceScore * CASE_PREFERENCE_WEIGHT;

        // Apply workload penalty for heavily loaded doctors
        totalScore = applyWorkloadPenalty(totalScore, doctor);

        // Apply urgency boost for emergency cases
        totalScore = applyUrgencyBoost(totalScore, medicalCase, doctor);

        // Ensure score is within bounds
        totalScore = Math.max(0.0, Math.min(100.0, totalScore));

        return DoctorMatchingResultDto.builder()
                .doctor(convertToLegacyDoctorDto(doctor))
                .doctorCapacity(doctor)
                .totalScore(totalScore)
                .scoreBreakdown(scoreBreakdown)
                .matchingReason(generateWorkloadAwareMatchingReason(scoreBreakdown, medicalCase, doctor))
                .workloadPercentage(doctor.getWorkloadPercentage())
                .emergencyMode(doctor.getEmergencyMode())
                .canAcceptImmediately(doctor.canAcceptCase())
                .assignmentSuccessProbability(calculateAssignmentSuccessProbability(doctor, medicalCase))
                .build();
    }

    /**
     * FIXED: Calculate disease expertise score using existing config service methods
     */
    private double calculateDiseaseExpertiseScoreFixed(Case medicalCase, DoctorCapacityDto doctor) {
        try {
            // Get disease information using existing config service method
            DiseaseDto disease = configService.getDiseaseByCode(medicalCase.getPrimaryDiseaseCode());

            if (disease != null) {
                // Get specializations for the disease using existing method
                List<String> diseaseSpecializations = configService.getSpecializationsForDisease(medicalCase.getPrimaryDiseaseCode());

                if (diseaseSpecializations != null && diseaseSpecializations.contains(doctor.getPrimarySpecialization())) {
                    return 85.0; // Strong disease expertise match
                }

                // Check if doctor's subspecializations match disease requirements
                if (doctor.getSubSpecializations() != null && diseaseSpecializations != null) {
                    boolean hasSubSpecMatch = doctor.getSubSpecializations().stream()
                            .anyMatch(diseaseSpecializations::contains);
                    if (hasSubSpecMatch) {
                        return 70.0; // Good subspecialization match
                    }
                }
            }

            // Check secondary diseases if primary doesn't match well
            if (medicalCase.getSecondaryDiseaseCodes() != null) {
                for (String diseaseCode : medicalCase.getSecondaryDiseaseCodes()) {
                    try {
                        List<String> secondaryDiseaseSpecs = configService.getSpecializationsForDisease(diseaseCode);
                        if (secondaryDiseaseSpecs != null && secondaryDiseaseSpecs.contains(doctor.getPrimarySpecialization())) {
                            return 60.0; // Moderate disease expertise for secondary diseases
                        }
                    } catch (Exception e) {
                        log.debug("Could not get specializations for secondary disease: {}", diseaseCode);
                    }
                }
            }

            return 40.0; // Basic expertise assumed if specialization matches case requirement
        } catch (Exception e) {
            log.warn("Error calculating disease expertise score for disease {}: {}",
                    medicalCase.getPrimaryDiseaseCode(), e.getMessage());
            return 50.0; // Neutral score on error
        }
    }

    /**
     * FIXED: Calculate symptom expertise score using simplified approach
     */
    private double calculateSymptomExpertiseScoreFixed(Case medicalCase, DoctorCapacityDto doctor) {
        try {
            if (medicalCase.getSymptomCodes() == null || medicalCase.getSymptomCodes().isEmpty()) {
                return 50.0; // Neutral if no symptoms specified
            }

            // Simplified approach: if doctor's specialization matches case requirement,
            // assume they have symptom expertise
            if (doctor.getPrimarySpecialization().equals(medicalCase.getRequiredSpecialization())) {
                return 75.0; // Good symptom-specialization match
            }

            // Check if any subspecializations match secondary specializations needed for the case
            if (doctor.getSubSpecializations() != null && medicalCase.getSecondarySpecializations() != null) {
                boolean hasMatch = doctor.getSubSpecializations().stream()
                        .anyMatch(subSpec -> medicalCase.getSecondarySpecializations().contains(subSpec));
                if (hasMatch) {
                    return 65.0; // Moderate symptom expertise
                }
            }

            return 45.0; // Limited symptom expertise
        } catch (Exception e) {
            log.warn("Error calculating symptom expertise score: {}", e.getMessage());
            return 50.0; // Neutral score on error
        }
    }

    /**
     * Calculate workload and availability score (major factor)
     */
    private double calculateWorkloadAvailabilityScore(Case medicalCase, DoctorCapacityDto doctor) {
        double score = 0.0;

        // Base availability score
        if (!doctor.getIsAvailable()) {
            return 0.0; // Not available = zero score
        }
        score += 20.0; // Base score for being available

        // Workload-based scoring (inverse relationship)
        double workloadPercentage = doctor.getWorkloadPercentage();
        if (workloadPercentage <= 30.0) {
            score += 50.0; // Very low workload - excellent
        } else if (workloadPercentage <= 50.0) {
            score += 40.0; // Low workload - very good
        } else if (workloadPercentage <= 70.0) {
            score += 25.0; // Moderate workload - good
        } else if (workloadPercentage <= 90.0) {
            score += 10.0; // High workload - limited
        } else {
            score += 0.0; // Very high workload - poor choice
        }

        // Active cases capacity score
        double caseCapacityRatio = (double) doctor.getActiveCases() / doctor.getMaxActiveCases();
        if (caseCapacityRatio <= 0.5) {
            score += 20.0; // Plenty of case capacity
        } else if (caseCapacityRatio <= 0.7) {
            score += 15.0; // Good case capacity
        } else if (caseCapacityRatio <= 0.9) {
            score += 5.0; // Limited case capacity
        }

        // Daily appointment capacity score
        double appointmentCapacityRatio = (double) doctor.getTodayAppointments() / doctor.getMaxDailyAppointments();
        if (appointmentCapacityRatio <= 0.5) {
            score += 10.0; // Good appointment availability
        } else if (appointmentCapacityRatio <= 0.8) {
            score += 5.0; // Limited appointment availability
        }

        // Emergency mode bonus (can handle overload)
        if (doctor.getEmergencyMode() && isEmergencyCase(medicalCase)) {
            score += 15.0; // Emergency mode bonus for urgent cases
        }

        return Math.min(100.0, score);
    }

    /**
     * Calculate specialization matching score
     */
    private double calculateSpecializationScore(Case medicalCase, DoctorCapacityDto doctor) {
        double score = 0.0;

        // Primary specialization exact match
        if (medicalCase.getRequiredSpecialization().equals(doctor.getPrimarySpecialization())) {
            score += 80.0;
        }

        // Secondary specializations match
        if (medicalCase.getSecondarySpecializations() != null && doctor.getSubSpecializations() != null) {
            Set<String> caseSecondarySpecs = medicalCase.getSecondarySpecializations();
            Set<String> doctorSubSpecs = doctor.getSubSpecializations();

            long matchingSecondarySpecs = caseSecondarySpecs.stream()
                    .mapToLong(spec -> doctorSubSpecs.contains(spec) ? 1 : 0)
                    .sum();

            if (matchingSecondarySpecs > 0) {
                score += Math.min(20.0, matchingSecondarySpecs * 10.0);
            }
        }

        return Math.min(100.0, score);
    }

    /**
     * Calculate experience-based score
     */
    private double calculateExperienceScore(DoctorCapacityDto doctor) {
        double score = 0.0;

        // Consultation count score
        int consultations = doctor.getConsultationCount();
        if (consultations >= 1000) {
            score += 40.0; // Very experienced
        } else if (consultations >= 500) {
            score += 30.0; // Experienced
        } else if (consultations >= 100) {
            score += 20.0; // Moderately experienced
        } else if (consultations >= 50) {
            score += 15.0; // Some experience
        } else {
            score += 10.0; // New doctor
        }

        // Rating-based experience bonus
        if (doctor.getAverageRating() != null) {
            if (doctor.getAverageRating() >= 4.5) {
                score += 30.0; // Excellent rating
            } else if (doctor.getAverageRating() >= 4.0) {
                score += 20.0; // Good rating
            } else if (doctor.getAverageRating() >= 3.5) {
                score += 10.0; // Average rating
            }
        } else {
            score += 15.0; // No rating yet (neutral)
        }

        return Math.min(100.0, score);
    }

    /**
     * Calculate performance-based score
     */
    private double calculatePerformanceScore(DoctorCapacityDto doctor) {
        double score = 50.0; // Base neutral score

        // Rating performance
        if (doctor.getAverageRating() != null) {
            score = doctor.getAverageRating() * 20.0; // Scale 0-5 rating to 0-100
        }

        // Consultation volume bonus (indicates reliability)
        if (doctor.getConsultationCount() > 100) {
            score += 10.0; // Volume bonus
        }

        // Completion rate bonus
        if (doctor.getCompletionRate() != null) {
            score += (doctor.getCompletionRate() - 80.0) * 0.5; // Bonus for high completion rates
        }

        return Math.min(100.0, Math.max(0.0, score));
    }

    /**
     * Calculate case preference score
     */
    private double calculateCasePreferenceScore(Case medicalCase, DoctorCapacityDto doctor) {
        double score = 50.0; // Neutral base

        // Emergency case preference
        if (isEmergencyCase(medicalCase) && doctor.getEmergencyMode()) {
            score += 25.0; // Prefers emergency cases
        }

        // Experience with case complexity
        if (doctor.getYearsOfExperience() != null && doctor.getYearsOfExperience() > 5) {
            score += 15.0; // Experienced doctors get preference bonus
        }

        return Math.min(100.0, score);
    }

    /**
     * Apply workload penalty for heavily loaded doctors
     */
    private double applyWorkloadPenalty(double baseScore, DoctorCapacityDto doctor) {
        if (doctor.getWorkloadPercentage() > workloadPenaltyThreshold) {
            double penaltyFactor = 1.0 - ((doctor.getWorkloadPercentage() - workloadPenaltyThreshold) / 100.0);
            return baseScore * Math.max(0.3, penaltyFactor); // Minimum 30% of original score
        }
        return baseScore;
    }

    /**
     * Apply urgency boost for emergency cases
     */
    private double applyUrgencyBoost(double baseScore, Case medicalCase, DoctorCapacityDto doctor) {
        if (isEmergencyCase(medicalCase)) {
            if (doctor.getEmergencyMode()) {
                return baseScore * 1.2; // 20% boost for emergency-enabled doctors
            } else if (doctor.getWorkloadPercentage() < 60.0) {
                return baseScore * 1.1; // 10% boost for low-workload doctors
            }
        }
        return baseScore;
    }

    /**
     * Calculate assignment success probability
     */
    private Double calculateAssignmentSuccessProbability(DoctorCapacityDto doctor, Case medicalCase) {
        double probability = 0.5; // Base 50%

        // Availability factor
        if (doctor.getIsAvailable()) probability += 0.2;

        // Workload factor
        if (doctor.getWorkloadPercentage() < 50.0) probability += 0.15;
        else if (doctor.getWorkloadPercentage() > 85.0) probability -= 0.15;

        // Emergency mode factor
        if (doctor.getEmergencyMode() && isEmergencyCase(medicalCase)) probability += 0.1;

        // Experience factor
        if (doctor.getConsultationCount() > 100) probability += 0.05;

        return Math.max(0.0, Math.min(1.0, probability));
    }

    /**
     * Select best doctors with workload balancing
     */
    private List<DoctorMatchingResultDto> selectBestDoctorsWithWorkloadBalance(
            Case medicalCase, List<DoctorMatchingResultDto> matchingResults) {

        // Sort by score descending, then by workload ascending
        List<DoctorMatchingResultDto> sortedResults = matchingResults.stream()
                .sorted((a, b) -> {
                    int scoreCompare = Double.compare(b.getTotalScore(), a.getTotalScore());
                    if (scoreCompare == 0) {
                        return Double.compare(a.getWorkloadPercentage(), b.getWorkloadPercentage());
                    }
                    return scoreCompare;
                })
                .collect(Collectors.toList());

        List<DoctorMatchingResultDto> selectedDoctors = new ArrayList<>();

        // Always try to assign primary doctor
        if (!sortedResults.isEmpty()) {
            DoctorMatchingResultDto primary = sortedResults.get(0);
            primary.setPriority(AssignmentPriority.PRIMARY);
            selectedDoctors.add(primary);

            log.info("Selected primary doctor {} with score {} and workload {}%",
                    primary.getDoctorCapacity().getDoctorId(), primary.getTotalScore(), primary.getWorkloadPercentage());
        }

        // Select additional doctors with workload consideration
        int maxDoctors = medicalCase.getMaxDoctorsAllowed();
        int minDoctors = medicalCase.getMinDoctorsRequired();

        for (int i = 1; i < sortedResults.size() && selectedDoctors.size() < maxDoctors; i++) {
            DoctorMatchingResultDto candidate = sortedResults.get(i);

            if (isDiverseAndBalancedSelection(selectedDoctors, candidate)) {
                candidate.setPriority(selectedDoctors.size() == 1 ?
                        AssignmentPriority.SECONDARY : AssignmentPriority.CONSULTANT);
                selectedDoctors.add(candidate);

                log.info("Selected additional doctor {} with score {} and workload {}%",
                        candidate.getDoctorCapacity().getDoctorId(), candidate.getTotalScore(), candidate.getWorkloadPercentage());
            }
        }

        // Ensure minimum doctors requirement
        if (selectedDoctors.size() < minDoctors) {
            for (DoctorMatchingResultDto result : sortedResults) {
                if (selectedDoctors.size() >= minDoctors) break;

                boolean alreadySelected = selectedDoctors.stream()
                        .anyMatch(selected -> selected.getDoctorCapacity().getDoctorId().equals(result.getDoctorCapacity().getDoctorId()));

                if (!alreadySelected && result.getTotalScore() >= (minimumScoreThreshold * 0.7)) {
                    result.setPriority(AssignmentPriority.CONSULTANT);
                    selectedDoctors.add(result);
                }
            }
        }

        return selectedDoctors;
    }

    /**
     * Check workload balance in selection
     */
    private boolean isDiverseAndBalancedSelection(List<DoctorMatchingResultDto> selected,
                                                  DoctorMatchingResultDto candidate) {
        if (selected.isEmpty()) return true;

        // Avoid multiple overloaded doctors
        long overloadedCount = selected.stream()
                .mapToLong(result -> result.getWorkloadPercentage() > 80.0 ? 1 : 0)
                .sum();

        if (overloadedCount >= 1 && candidate.getWorkloadPercentage() > 80.0) {
            return false;
        }

        // Check specialization diversity
        Set<String> selectedSpecs = selected.stream()
                .map(result -> result.getDoctorCapacity().getPrimarySpecialization())
                .collect(Collectors.toSet());

        return !selectedSpecs.contains(candidate.getDoctorCapacity().getPrimarySpecialization()) ||
                selected.size() < 2;
    }

    // [Continue with rest of the methods - createCaseAssignmentsWithWorkloadUpdate, validation methods, etc.]
    // These remain the same as in the previous version

    /**
     * Create case assignments with workload updates
     */
    private List<CaseAssignment> createCaseAssignmentsWithWorkloadUpdate(
            Case medicalCase, List<DoctorMatchingResultDto> selectedDoctors) {

        List<CaseAssignment> assignments = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (DoctorMatchingResultDto result : selectedDoctors) {
            CaseAssignment assignment = CaseAssignment.builder()
                    .caseEntity(medicalCase)
                    .doctorId(result.getDoctorCapacity().getDoctorId())
                    .status(AssignmentStatus.PENDING)
                    .priority(result.getPriority())
                    .assignedAt(now)
                    .expiresAt(calculateExpirationTime(medicalCase.getUrgencyLevel()))
                    .assignmentReason(result.getMatchingReason())
                    .matchingScore(result.getTotalScore())
                    .build();

            assignments.add(caseAssignmentRepository.save(assignment));

            // Update doctor's workload
            try {
                doctorServiceClient.updateDoctorWorkload(result.getDoctorCapacity().getDoctorId());
                log.info("Triggered workload update for doctor {} after case assignment",
                        result.getDoctorCapacity().getDoctorId());
            } catch (Exception e) {
                log.error("Failed to update doctor workload for doctor {}: {}",
                        result.getDoctorCapacity().getDoctorId(), e.getMessage());
            }
        }

        return assignments;
    }

    /**
     * Check if doctor is eligible for case
     */
    private boolean isDoctorEligibleForCase(Case medicalCase, DoctorCapacityDto doctor) {
        if (!doctor.getIsAvailable()) return false;

        boolean alreadyAssigned = caseAssignmentRepository.existsByCaseEntityIdAndDoctorId(
                medicalCase.getId(), doctor.getDoctorId());
        if (alreadyAssigned) return false;

        if (!doctor.getEmergencyMode()) {
            if (doctor.getActiveCases() >= doctor.getMaxActiveCases()) return false;
            if (doctor.getTodayAppointments() >= doctor.getMaxDailyAppointments()) return false;
        }

        return true;
    }

    /**
     * Check if case is emergency
     */
    private boolean isEmergencyCase(Case medicalCase) {
        return medicalCase.getUrgencyLevel() == UrgencyLevel.CRITICAL ||
                medicalCase.getUrgencyLevel() == UrgencyLevel.HIGH;
    }

    /**
     * Generate workload-aware matching reason
     */
    private String generateWorkloadAwareMatchingReason(Map<String, Double> scoreBreakdown,
                                                       Case medicalCase, DoctorCapacityDto doctor) {
        StringBuilder reason = new StringBuilder("Matched based on: ");

        List<String> strengths = new ArrayList<>();

        if (scoreBreakdown.get("specialization") >= 70) {
            strengths.add("Strong specialization match");
        }
        if (scoreBreakdown.get("workload_availability") >= 70) {
            strengths.add("Excellent availability & low workload");
        } else if (scoreBreakdown.get("workload_availability") >= 50) {
            strengths.add("Good availability");
        }
        if (scoreBreakdown.get("disease_expertise") >= 60) {
            strengths.add("Disease expertise");
        }
        if (scoreBreakdown.get("experience") >= 70) {
            strengths.add("Extensive experience");
        }
        if (doctor.getEmergencyMode() && isEmergencyCase(medicalCase)) {
            strengths.add("Emergency mode enabled");
        }

        if (strengths.isEmpty()) {
            reason.append("General qualification and capacity");
        } else {
            reason.append(String.join(", ", strengths));
        }

        reason.append(String.format(" (Workload: %.1f%%)", doctor.getWorkloadPercentage()));
        return reason.toString();
    }

    /**
     * Convert DoctorCapacityDto to legacy DoctorDto for compatibility
     */
    private DoctorDto convertToLegacyDoctorDto(DoctorCapacityDto capacity) {
        DoctorDto legacy = new DoctorDto();
        legacy.setUserId(capacity.getDoctorId());
        legacy.setFullName(capacity.getFullName());
        legacy.setPrimarySpecialization(capacity.getPrimarySpecialization());
        legacy.setRating(capacity.getAverageRating());
        legacy.setConsultationCount(capacity.getConsultationCount());
        legacy.setIsAvailable(capacity.getIsAvailable());
        legacy.setYearsOfExperience(capacity.getYearsOfExperience());

        // Convert Set to List for sub-specializations if needed
        if (capacity.getSubSpecializations() != null) {
            legacy.setSubSpecializations(capacity.getSubSpecializations());
        }

        return legacy;
    }

    /**
     * Calculate assignment expiration time based on urgency
     */
    private LocalDateTime calculateExpirationTime(UrgencyLevel urgency) {
        LocalDateTime now = LocalDateTime.now();
        return switch (urgency) {
            case CRITICAL -> now.plusHours(1);   // 1 hour for critical
            case HIGH -> now.plusHours(4);       // 4 hours for high
            case MEDIUM -> now.plusHours(12);    // 12 hours for medium
            case LOW -> now.plusHours(24);       // 24 hours for low
        };
    }

    /**
     * Validate that a case can be assigned
     */
    private void validateCaseForAssignment(Case medicalCase) {
        if (medicalCase.getStatus() != CaseStatus.PENDING && medicalCase.getStatus() != CaseStatus.SUBMITTED) {
            throw new BusinessException("Case is not in a state that allows assignment", HttpStatus.BAD_REQUEST);
        }

        Patient patient = medicalCase.getPatient();
        if (patient.getSubscriptionStatus() != SubscriptionStatus.ACTIVE) {
            throw new BusinessException("Patient must have active subscription", HttpStatus.PAYMENT_REQUIRED);
        }

        if (patient.getSubscriptionExpiry() != null &&
                patient.getSubscriptionExpiry().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Patient subscription has expired", HttpStatus.PAYMENT_REQUIRED);
        }
    }

    /**
     * Update case after successful assignment
     */
    private void updateCaseAfterAssignment(Case medicalCase, List<CaseAssignment> assignments) {
        medicalCase.setStatus(CaseStatus.ASSIGNED);
        medicalCase.setFirstAssignedAt(LocalDateTime.now());
        medicalCase.setLastAssignedAt(LocalDateTime.now());
        medicalCase.setAssignmentAttempts(medicalCase.getAssignmentAttempts() + 1);
        caseRepository.save(medicalCase);
    }

    /**
     * Send assignment notifications
     */
    private void sendAssignmentNotifications(Case medicalCase, List<CaseAssignment> assignments) {
        for (CaseAssignment assignment : assignments) {
            try {
                String title = String.format("New %s Case Assignment",
                        assignment.getPriority().name().toLowerCase());
                String message = String.format(
                        "You have been assigned a %s case: %s. Urgency: %s. Please review and respond by %s.",
                        assignment.getPriority().name().toLowerCase(),
                        medicalCase.getCaseTitle(),
                        medicalCase.getUrgencyLevel().name(),
                        assignment.getExpiresAt().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
                );

                patientEventProducer.sendAssignmentNotification(
                        medicalCase.getPatient().getUserId(),
                        assignment.getDoctorId(),
                        medicalCase.getId(),
                        title,
                        message
                );
            } catch (Exception e) {
                log.error("Failed to send assignment notification for assignment {}", assignment.getId(), e);
            }
        }
    }

    /**
     * Accept assignment with workload update
     */
    @Transactional
    public void acceptAssignmentWithWorkloadUpdate(Long doctorId, Long assignmentId) {
        CaseAssignment caseAssignment = caseAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException("Assignment not found", HttpStatus.NOT_FOUND));

        if (!caseAssignment.getDoctorId().equals(doctorId)) {
            throw new BusinessException("Unauthorized access", HttpStatus.FORBIDDEN);
        }

        if (caseAssignment.getStatus() != AssignmentStatus.PENDING) {
            throw new BusinessException("Assignment cannot be accepted", HttpStatus.BAD_REQUEST);
        }

        // Check current workload capacity
        try {
            var capacityResponse = doctorServiceClient.getDoctorCapacity(doctorId);
            if (capacityResponse != null && capacityResponse.getBody() != null) {
                DoctorCapacityDto capacity = capacityResponse.getBody().getData();

                if (!capacity.getEmergencyMode() &&
                        capacity.getActiveCases() >= capacity.getMaxActiveCases()) {
                    throw new BusinessException("Doctor workload capacity exceeded", HttpStatus.BAD_REQUEST);
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Could not verify doctor capacity, proceeding with assignment: {}", e.getMessage());
        }

        caseAssignment.setStatus(AssignmentStatus.ACCEPTED);
        caseAssignment.setRespondedAt(LocalDateTime.now());
        caseAssignmentRepository.save(caseAssignment);

        // Update case status if primary assignment
        if (caseAssignment.getPriority() == AssignmentPriority.PRIMARY) {
            Case medicalCase = caseAssignment.getCaseEntity();
            medicalCase.setStatus(CaseStatus.ACCEPTED);
            caseRepository.save(medicalCase);
        }

        // Update doctor workload
        try {
            doctorServiceClient.updateDoctorWorkload(doctorId);
            log.info("Updated workload for doctor {} after case acceptance", doctorId);
        } catch (Exception e) {
            log.error("Failed to update doctor workload after acceptance: {}", e.getMessage());
        }
    }

    /**
     * Reject assignment with workload update and automatic reassignment
     */
    @Transactional
    public void rejectAssignmentWithWorkloadUpdate(Long doctorId, Long assignmentId, String reason) {
        CaseAssignment assignment = caseAssignmentRepository.findById(assignmentId)
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

        Case medicalCase = assignment.getCaseEntity();
        medicalCase.setRejectionCount(medicalCase.getRejectionCount() + 1);
        caseRepository.save(medicalCase);

        // Update doctor workload
        try {
            doctorServiceClient.updateDoctorWorkload(doctorId);
            log.info("Updated workload for doctor {} after case rejection", doctorId);
        } catch (Exception e) {
            log.error("Failed to update doctor workload after rejection: {}", e.getMessage());
        }

        // Handle automatic reassignment
        handleCaseReassignmentAfterRejection(medicalCase, assignment);
    }

    /**
     * Handle case reassignment after rejection
     */
    private void handleCaseReassignmentAfterRejection(Case medicalCase, CaseAssignment rejectedAssignment) {
        // Check if we still have enough doctors assigned
        long activeAssignments = caseAssignmentRepository.findByCaseEntityId(medicalCase.getId())
                .stream()
                .filter(a -> a.getStatus() == AssignmentStatus.PENDING || a.getStatus() == AssignmentStatus.ACCEPTED)
                .count();

        if (activeAssignments < medicalCase.getMinDoctorsRequired()) {
            log.info("Case {} below minimum doctor requirement after rejection, attempting reassignment",
                    medicalCase.getId());

            try {
                // Find replacement doctor
                List<DoctorCapacityDto> availableDoctors = findEligibleDoctorsWithWorkload(medicalCase);

                // Filter out already assigned doctors
                Set<Long> assignedDoctorIds = caseAssignmentRepository.findByCaseEntityId(medicalCase.getId())
                        .stream()
                        .map(CaseAssignment::getDoctorId)
                        .collect(Collectors.toSet());

                List<DoctorCapacityDto> newCandidates = availableDoctors.stream()
                        .filter(doctor -> !assignedDoctorIds.contains(doctor.getDoctorId()))
                        .collect(Collectors.toList());

                if (!newCandidates.isEmpty()) {
                    List<DoctorMatchingResultDto> matchingResults = calculateWorkloadAwareMatchingScores(
                            medicalCase, newCandidates);

                    if (!matchingResults.isEmpty()) {
                        DoctorMatchingResultDto best = matchingResults.stream()
                                .max(Comparator.comparing(DoctorMatchingResultDto::getTotalScore))
                                .orElse(null);

                        if (best != null) {
                            // Create replacement assignment
                            CaseAssignment replacement = CaseAssignment.builder()
                                    .caseEntity(medicalCase)
                                    .doctorId(best.getDoctorCapacity().getDoctorId())
                                    .status(AssignmentStatus.PENDING)
                                    .priority(rejectedAssignment.getPriority())
                                    .assignedAt(LocalDateTime.now())
                                    .expiresAt(calculateExpirationTime(medicalCase.getUrgencyLevel()))
                                    .assignmentReason("Replacement after rejection: " + best.getMatchingReason())
                                    .matchingScore(best.getTotalScore())
                                    .build();

                            caseAssignmentRepository.save(replacement);

                            // Update workload for new doctor
                            doctorServiceClient.updateDoctorWorkload(best.getDoctorCapacity().getDoctorId());

                            log.info("Successfully assigned replacement doctor {} for case {} after rejection",
                                    best.getDoctorCapacity().getDoctorId(), medicalCase.getId());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to find replacement doctor for case {} after rejection: {}",
                        medicalCase.getId(), e.getMessage());
            }
        }
    }

    /**
     * Claim case with workload validation
     */
    @Transactional
    public void claimCaseWithWorkloadValidation(Long caseId, Long doctorId, String note) {
        try {
            log.info("Doctor {} attempting to claim case {}", doctorId, caseId);

            Case claimedCase = caseRepository.findById(caseId)
                    .orElseThrow(() -> new BusinessException("Case not found", HttpStatus.NOT_FOUND));

            if (claimedCase.getStatus() != CaseStatus.PENDING) {
                throw new BusinessException("Case cannot be claimed", HttpStatus.BAD_REQUEST);
            }

            // Validate doctor workload capacity
            try {
                var capacityResponse = doctorServiceClient.getDoctorCapacity(doctorId);
                if (capacityResponse != null && capacityResponse.getBody() != null) {
                    DoctorCapacityDto capacity = capacityResponse.getBody().getData();

                    if (!capacity.getIsAvailable()) {
                        throw new BusinessException("Doctor is not available", HttpStatus.BAD_REQUEST);
                    }

                    if (!capacity.getEmergencyMode() &&
                            capacity.getActiveCases() >= capacity.getMaxActiveCases()) {
                        throw new BusinessException("Doctor workload capacity exceeded", HttpStatus.BAD_REQUEST);
                    }
                }
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Could not verify doctor capacity, proceeding with caution: {}", e.getMessage());
            }

            claimedCase.setStatus(CaseStatus.ASSIGNED);
            claimedCase.setFirstAssignedAt(LocalDateTime.now());
            caseRepository.save(claimedCase);

            // Create case assignment
            createOrUpdateCaseAssignmentWithWorkload(caseId, doctorId, note);

            // Update doctor workload
            doctorServiceClient.updateDoctorWorkload(doctorId);

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

    /**
     * Create or update case assignment with workload tracking
     */
    private void createOrUpdateCaseAssignmentWithWorkload(Long caseId, Long doctorId, String note) {
        List<CaseAssignment> existingAssignments = caseAssignmentRepository.findByCaseEntityId(caseId);

        CaseAssignment assignment;
        if (existingAssignments.isEmpty()) {
            assignment = CaseAssignment.builder()
                    .caseEntity(caseRepository.getReferenceById(caseId))
                    .doctorId(doctorId)
                    .assignedAt(LocalDateTime.now())
                    .assignmentReason(note)
                    .status(AssignmentStatus.ACCEPTED)
                    .priority(AssignmentPriority.PRIMARY)
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

    /**
     * Handle expired assignments with workload updates and reassignment
     */
    @Scheduled(fixedDelay = 300000) // Check every 5 minutes
    public void handleExpiredAssignmentsWithWorkloadUpdate() {
        List<CaseAssignment> expiredAssignments = caseAssignmentRepository
                .findByStatusAndExpiresAtBefore(AssignmentStatus.PENDING, LocalDateTime.now());

        for (CaseAssignment assignment : expiredAssignments) {
            assignment.setStatus(AssignmentStatus.EXPIRED);
            caseAssignmentRepository.save(assignment);

            // Update doctor workload after assignment expiration
            try {
                doctorServiceClient.updateDoctorWorkload(assignment.getDoctorId());
                log.info("Updated workload for doctor {} after assignment expiration", assignment.getDoctorId());
            } catch (Exception e) {
                log.error("Failed to update doctor workload after expiration for doctor {}: {}",
                        assignment.getDoctorId(), e.getMessage());
            }

            // Attempt automatic reassignment for critical cases
            if (assignment.getCaseEntity().getUrgencyLevel() == UrgencyLevel.CRITICAL) {
                try {
                    log.info("Attempting automatic reassignment for critical case {} after expiration",
                            assignment.getCaseEntity().getId());
                    assignCaseToMultipleDoctors(assignment.getCaseEntity().getId());
                } catch (Exception e) {
                    log.error("Failed automatic reassignment for critical case {}: {}",
                            assignment.getCaseEntity().getId(), e.getMessage());
                }
            }
        }
    }

    /**
     * Get workload-aware assignment statistics
     */
    public Map<String, Object> getWorkloadAwareAssignmentStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Basic assignment statistics
            long totalPendingAssignments = caseAssignmentRepository.countByStatus(AssignmentStatus.PENDING);
            long totalAcceptedAssignments = caseAssignmentRepository.countByStatus(AssignmentStatus.ACCEPTED);
            long totalRejectedAssignments = caseAssignmentRepository.countByStatus(AssignmentStatus.REJECTED);
            long totalExpiredAssignments = caseAssignmentRepository.countByStatus(AssignmentStatus.EXPIRED);

            stats.put("pendingAssignments", totalPendingAssignments);
            stats.put("acceptedAssignments", totalAcceptedAssignments);
            stats.put("rejectedAssignments", totalRejectedAssignments);
            stats.put("expiredAssignments", totalExpiredAssignments);

            // Calculate acceptance rate
            long totalAssignments = totalAcceptedAssignments + totalRejectedAssignments + totalExpiredAssignments;
            double acceptanceRate = totalAssignments > 0 ? (double) totalAcceptedAssignments / totalAssignments * 100 : 0;
            stats.put("acceptanceRate", acceptanceRate);

            // Workload-related statistics
            try {
                var systemStatsResponse = doctorServiceClient.getSystemWorkloadStatistics();
                if (systemStatsResponse != null && systemStatsResponse.getBody() != null) {
                    stats.put("systemWorkloadStats", systemStatsResponse.getBody().getData());
                }
            } catch (Exception e) {
                log.warn("Could not retrieve system workload statistics: {}", e.getMessage());
            }

            stats.put("lastCalculated", LocalDateTime.now());

        } catch (Exception e) {
            log.error("Error calculating assignment statistics: {}", e.getMessage(), e);
        }

        return stats;
    }
}