
package com.patientservice.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OldSmartCaseAssignmentService {

//    private final DoctorServiceClient doctorServiceClient;
//    private final CaseRepository caseRepository;
//    private final CaseAssignmentRepository caseAssignmentRepository;
//    private final MedicalConfigurationService configService;
//    private final NotificationServiceClient notificationService;
//
//    private static final double PRIMARY_SPECIALIZATION_WEIGHT = 0.30;
//    private static final double DISEASE_EXPERTISE_WEIGHT = 0.25;
//    private static final double SYMPTOM_EXPERTISE_WEIGHT = 0.15;
//    private static final double EXPERIENCE_WEIGHT = 0.10;
//    private static final double AVAILABILITY_WEIGHT = 0.10;
//    private static final double PERFORMANCE_WEIGHT = 0.05;
//    private static final double CASE_PREFERENCE_WEIGHT = 0.05;
//    private final PatientEventProducer patientEventProducer;
//
//
//    /**
//     * Main method to assign a case to multiple doctors
//     */
//    public List<CaseAssignment> assignCaseToMultipleDoctors(Long caseId) {
//        log.info("Starting smart case assignment for case ID: {}", caseId);
//
//        Case medicalCase = caseRepository.findById(caseId)
//            .orElseThrow(() -> new BusinessException("Case not found", HttpStatus.NOT_FOUND));
//
//        // Validate case can be assigned
//        validateCaseForAssignment(medicalCase);
//
//        // Get eligible doctors
//        List<DoctorDto> eligibleDoctors = findEligibleDoctors(medicalCase);
//
//        if (eligibleDoctors.isEmpty()) {
//            throw new BusinessException("No eligible doctors found for this case", HttpStatus.NOT_FOUND);
//        }
//
//        // Calculate matching scores for all eligible doctors
//        List<DoctorMatchingResultDto> matchingResults = calculateMatchingScores(medicalCase, eligibleDoctors);
//
//        // Sort by score and select top doctors
//        List<DoctorMatchingResultDto> selectedDoctors = selectBestDoctors(medicalCase, matchingResults);
//
//        // Create assignments
//        List<CaseAssignment> assignments = createCaseAssignments(medicalCase, selectedDoctors);
//
//        // Update case status and metadata
//        updateCaseAfterAssignment(medicalCase, assignments);
//
//        // Send notifications
//        sendAssignmentNotifications(medicalCase, assignments);
//
//        log.info("Successfully assigned case {} to {} doctors", caseId, assignments.size());
//        return assignments;
//    }
//
//    /**
//     * Calculate comprehensive matching score between case and doctor
//     */
//    private DoctorMatchingResultDto calculateMatchingScore(Case medicalCase, DoctorDto doctor) {
//        double totalScore = 0.0;
//        Map<String, Double> scoreBreakdown = new HashMap<>();
//
//        // 1. Primary Specialization Match (30%)
//        double specializationScore = calculateSpecializationScore(medicalCase, doctor);
//        totalScore += specializationScore * PRIMARY_SPECIALIZATION_WEIGHT;
//        scoreBreakdown.put("specialization", specializationScore);
//
//        // 2. Disease Expertise Match (25%)
//        double diseaseScore = calculateDiseaseExpertiseScore(medicalCase, doctor);
//        totalScore += diseaseScore * DISEASE_EXPERTISE_WEIGHT;
//        scoreBreakdown.put("disease_expertise", diseaseScore);
//
//        // 3. Symptom Expertise Match (15%)
//        double symptomScore = calculateSymptomExpertiseScore(medicalCase, doctor);
//        totalScore += symptomScore * SYMPTOM_EXPERTISE_WEIGHT;
//        scoreBreakdown.put("symptom_expertise", symptomScore);
//
//        // 4. Experience Score (10%)
//        double experienceScore = calculateExperienceScore(medicalCase, doctor);
//        totalScore += experienceScore * EXPERIENCE_WEIGHT;
//        scoreBreakdown.put("experience", experienceScore);
//
//        // 5. Availability Score (10%)
//        double availabilityScore = calculateAvailabilityScore(doctor);
//        totalScore += availabilityScore * AVAILABILITY_WEIGHT;
//        scoreBreakdown.put("availability", availabilityScore);
//
//        // 6. Performance Score (5%)
//        double performanceScore = calculatePerformanceScore(doctor);
//        totalScore += performanceScore * PERFORMANCE_WEIGHT;
//        scoreBreakdown.put("performance", performanceScore);
//
//        // 7. Case Preference Score (5%)
//        double preferenceScore = calculateCasePreferenceScore(medicalCase, doctor);
//        totalScore += preferenceScore * CASE_PREFERENCE_WEIGHT;
//        scoreBreakdown.put("case_preference", preferenceScore);
//
//        // Apply case complexity and urgency modifiers
//        totalScore = applyComplexityModifier(totalScore, medicalCase, doctor);
//        totalScore = applyUrgencyModifier(totalScore, medicalCase, doctor);
//
//        return DoctorMatchingResultDto.builder()
//            .doctor(doctor)
//            .totalScore(Math.min(totalScore, 100.0)) // Cap at 100
//            .scoreBreakdown(scoreBreakdown)
//            .matchingReason(generateMatchingReason(scoreBreakdown, medicalCase, doctor))
//            .build();
//    }
//
//    /**
//     * Calculate specialization matching score
//     */
//    private double calculateSpecializationScore(Case medicalCase, DoctorDto doctor) {
//        double score = 0.0;
//
//        // Primary specialization exact match
//        if (doctor.getPrimarySpecialization().equals(medicalCase.getRequiredSpecialization())) {
//            score += 40.0;
//        }
//
//        // Check if doctor has required specialization in their list
//        if (doctor.getSpecializationCodes().contains(medicalCase.getRequiredSpecialization())) {
//            score += 30.0;
//        }
//
//        // Secondary specializations match
//        Set<String> matchingSecondary = new HashSet<>(doctor.getSpecializationCodes());
//        matchingSecondary.retainAll(medicalCase.getSecondarySpecializations());
//        score += (matchingSecondary.size() * 5.0); // 5 points per secondary match
//
//        // Subspecialization bonus
//        List<String> diseaseSpecializations = configService.getSpecializationsForDisease(medicalCase.getPrimaryDiseaseCode());
//        Set<String> matchingSubSpec = new HashSet<>(doctor.getSubSpecializationCodes());
//        matchingSubSpec.retainAll(diseaseSpecializations);
//        score += (matchingSubSpec.size() * 10.0); // 10 points per subspecialization match
//
//        return Math.min(score, 100.0);
//    }
//
//    /**
//     * Calculate disease expertise matching score
//     */
//    private double calculateDiseaseExpertiseScore(Case medicalCase, DoctorDto doctor) {
//        double score = 0.0;
//
//        // Primary disease expertise
//        if (doctor.getDiseaseExpertiseCodes().contains(medicalCase.getPrimaryDiseaseCode())) {
//            score += 50.0;
//        }
//
//        // Secondary diseases
//        Set<String> matchingDiseases = new HashSet<>(doctor.getDiseaseExpertiseCodes());
//        matchingDiseases.retainAll(medicalCase.getSecondaryDiseaseCodes());
//        score += (matchingDiseases.size() * 10.0);
//
//        // Related disease categories
//        DiseaseDto primaryDisease = configService.getDiseaseByCode(medicalCase.getPrimaryDiseaseCode());
//        long categoryMatches = doctor.getDiseaseExpertiseCodes().stream()
//            .mapToLong(code -> {
//                try {
//                    DiseaseDto expertiseDisease = configService.getDiseaseByCode(code);
//                    return expertiseDisease.getCategory().equals(primaryDisease.getCategory()) ? 1 : 0;
//                } catch (Exception e) {
//                    return 0;
//                }
//            })
//            .sum();
//        score += (categoryMatches * 5.0);
//
//        return Math.min(score, 100.0);
//    }
//
//    /**
//     * Calculate symptom expertise matching score
//     */
//    private double calculateSymptomExpertiseScore(Case medicalCase, DoctorDto doctor) {
//        Set<String> caseSymptoms = medicalCase.getSymptomCodes();
//        Set<String> doctorSymptoms = doctor.getSymptomExpertiseCodes();
//
//        if (caseSymptoms.isEmpty() || doctorSymptoms.isEmpty()) {
//            return 50.0; // Neutral score if no data
//        }
//
//        Set<String> matchingSymptoms = new HashSet<>(caseSymptoms);
//        matchingSymptoms.retainAll(doctorSymptoms);
//
//        double matchPercentage = (double) matchingSymptoms.size() / caseSymptoms.size();
//        return matchPercentage * 100.0;
//    }
//
//    /**
//     * Calculate experience-based score
//     */
//    private double calculateExperienceScore(Case medicalCase, DoctorDto doctor) {
//        int experience = doctor.getYearsOfExperience() != null ? doctor.getYearsOfExperience() : 0;
//
//        // Base score based on experience
//        double score = Math.min(experience * 2.0, 60.0); // Max 60 for 30+ years
//
//        // Complexity bonus
//        if (medicalCase.getComplexity() == CaseComplexity.HIGHLY_COMPLEX && experience >= 10) {
//            score += 20.0;
//        } else if (medicalCase.getComplexity() == CaseComplexity.COMPLEX && experience >= 5) {
//            score += 15.0;
//        }
//
//        // Consultation count bonus
//        if (doctor.getTotalConsultations() > 100) {
//            score += 10.0;
//        } else if (doctor.getTotalConsultations() > 50) {
//            score += 5.0;
//        }
//
//        return Math.min(score, 100.0);
//    }
//
//    /**
//     * Calculate availability score
//     */
//    private double calculateAvailabilityScore(DoctorDto doctor) {
//        if (!doctor.getIsAvailable()) {
//            return 0.0;
//        }
//
//        double score = 50.0; // Base score for being available
//
//        // Case load factor
//        double loadPercentage = (double) doctor.getCurrentCaseLoad() / doctor.getMaxConcurrentCases();
//        if (loadPercentage < 0.5) {
//            score += 40.0; // Low load bonus
//        } else if (loadPercentage < 0.8) {
//            score += 20.0; // Medium load
//        } else if (loadPercentage < 1.0) {
//            score += 10.0; // High load penalty
//        } else {
//            return 0.0; // Overloaded
//        }
//
//        // Response time bonus
//        if (doctor.getAverageResponseTime() <= 2.0) {
//            score += 10.0; // Fast responder
//        } else if (doctor.getAverageResponseTime() <= 6.0) {
//            score += 5.0; // Good responder
//        }
//
//        return Math.min(score, 100.0);
//    }
//
//    /**
//     * Calculate performance score
//     */
//    private double calculatePerformanceScore(DoctorDto doctor) {
//        double score = 0.0;
//
//        // Rating score (0-5 scale to 0-50 points)
//        if (doctor.getAverageRating() != null) {
//            score += (doctor.getAverageRating() / 5.0) * 50.0;
//        }
//
//        // Acceptance rate (if they have history)
//        int totalOffered = doctor.getAcceptedCases() + doctor.getRejectedCases();
//        if (totalOffered > 0) {
//            double acceptanceRate = (double) doctor.getAcceptedCases() / totalOffered;
//            score += acceptanceRate * 30.0;
//        } else {
//            score += 25.0; // Neutral score for new doctors
//        }
//
//        // Completion rate bonus
//        if (doctor.getTotalConsultations() > 10) {
//            score += 20.0; // Experienced doctor bonus
//        }
//
//        return Math.min(score, 100.0);
//    }
//
//    /**
//     * Calculate case preference score
//     */
//    private double calculateCasePreferenceScore(Case medicalCase, DoctorDto doctor) {
//        double score = 50.0; // Neutral base
//
//        // Complexity preference
//        if (medicalCase.getComplexity().ordinal() <= doctor.getMaxComplexityLevel().ordinal()) {
//            score += 20.0;
//        } else {
//            return 0.0; // Cannot handle this complexity
//        }
//
//        // Urgency preference
//        if (medicalCase.getUrgencyLevel() == UrgencyLevel.CRITICAL ||
//            medicalCase.getUrgencyLevel() == UrgencyLevel.HIGH) {
//            if (doctor.getAcceptsUrgentCases()) {
//                score += 15.0;
//            } else {
//                score -= 20.0;
//            }
//        }
//
//        // Second opinion preference
//        if (medicalCase.getRequiresSecondOpinion() && doctor.getAcceptsSecondOpinions()) {
//            score += 15.0;
//        } else if (medicalCase.getRequiresSecondOpinion() && !doctor.getAcceptsSecondOpinions()) {
//            score -= 30.0;
//        }
//
//        return Math.max(score, 0.0);
//    }
//
//    /**
//     * Apply complexity modifier to total score
//     */
//    private double applyComplexityModifier(double baseScore, Case medicalCase, DoctorDto doctor) {
//        if (medicalCase.getComplexity() == CaseComplexity.HIGHLY_COMPLEX) {
//            if (doctor.getAcceptsComplexCases() && doctor.getYearsOfExperience() >= 10) {
//                return baseScore * 1.1; // 10% bonus for complex case expertise
//            } else if (!doctor.getAcceptsComplexCases()) {
//                return baseScore * 0.5; // Significant penalty
//            }
//        }
//        return baseScore;
//    }
//
//    /**
//     * Apply urgency modifier to total score
//     */
//    private double applyUrgencyModifier(double baseScore, Case medicalCase, DoctorDto doctor) {
//        if (medicalCase.getUrgencyLevel() == UrgencyLevel.CRITICAL) {
//            if (doctor.getAcceptsUrgentCases() && doctor.getAverageResponseTime() <= 2.0) {
//                return baseScore * 1.15; // 15% bonus for urgent case handling
//            } else if (!doctor.getAcceptsUrgentCases()) {
//                return baseScore * 0.3; // Major penalty
//            }
//        }
//        return baseScore;
//    }
//
//    /**
//     * Find all eligible doctors for a case
//     */
//    private List<DoctorDto> findEligibleDoctors(Case medicalCase) {
//        // Get all verified and available doctors
//        List<DoctorDto> allDoctors = new ArrayList<>();
//        try{
//            List<DoctorDto> doctors = doctorServiceClient.findByVerificationStatusAndIsAvailableTrue(
//                    VerificationStatus.VERIFIED, true).getBody().getData();
//            allDoctors = doctors.stream().map(this::convertToDoctorDto).toList();
//        }catch (Exception e) {
//            log.error("Failed to fetch doctors ...");
//            log.error(e.getMessage());
//        }
//
//        return allDoctors.stream()
//            .filter(doctor -> isDoctorEligible(doctor, medicalCase))
//            .collect(Collectors.toList());
//    }
//
//    public DoctorDto convertToDoctorDto(DoctorDto doctor){
//        DoctorDto doctorDto = new DoctorDto();
//          doctorDto.setDoctorId(doctor.getDoctorId());
//          doctorDto.setFullName(doctor.getFullName());
//          doctorDto.setPrimarySpecializationCode(doctor.getPrimarySpecializationCode());
//          doctorDto.setSpecializationCodes(doctor.getSpecializationCodes());
//          doctorDto.setSubSpecializationCodes(doctor.getSubSpecializationCodes());
//          doctorDto.setDiseaseExpertiseCodes(doctor.getDiseaseExpertiseCodes());
//          doctorDto.setSymptomExpertiseCodes(doctor.getSymptomExpertiseCodes());
//          doctorDto.setYearsOfExperience(doctor.getYearsOfExperience());
//          doctorDto.setCertifications(doctor.getCertifications());
//          doctorDto.setResearchAreas(doctor.getResearchAreas());
//          doctorDto.setMaxConcurrentCases(doctor.getMaxConcurrentCases());
//          doctorDto.setCurrentCaseLoad(doctor.getCurrentCaseLoad());
//          doctorDto.setAcceptsSecondOpinions(doctor.getAcceptsSecondOpinions());
//          doctorDto.setAcceptsComplexCases(doctor.getAcceptsComplexCases());
//          doctorDto.setAverageRating(doctor.getAverageRating());
//          doctorDto.setTotalConsultations(doctor.getTotalConsultations());
//          doctorDto.setAcceptedCases(doctor.getAcceptedCases());
//          doctorDto.setRejectedCases(doctor.getRejectedCases());
//          doctorDto.setAverageResponseTime(doctor.getAverageResponseTime());
//          doctorDto.setPreferredCaseTypes(doctor.getPreferredCaseTypes());
//          doctorDto.setMaxComplexityLevel(doctor.getMaxComplexityLevel());
//          doctorDto.setAcceptsUrgentCases(doctor.getAcceptsUrgentCases());
//          doctorDto.setBaseConsultationFee(doctor.getBaseConsultationFee());
//          doctorDto.setUrgentCaseFee(doctor.getUrgentCaseFee());
//          doctorDto.setComplexCaseFee(doctor.getComplexCaseFee());
//          doctorDto.setIsAvailable(doctor.getIsAvailable());
//
//        return doctorDto;
//    }
//
//    /**
//     * Check if a doctor is eligible for a specific case
//     */
//    private boolean isDoctorEligible(DoctorDto doctor, Case medicalCase) {
//        // Basic availability check
//        if (!doctor.getIsAvailable() || doctor.getCurrentCaseLoad() >= doctor.getMaxConcurrentCases()) {
//            return false;
//        }
//
//        // Specialization requirement
//        boolean hasRequiredSpec = doctor.getPrimarySpecializationCode().equals(medicalCase.getRequiredSpecialization()) ||
//                                doctor.getSpecializationCodes().contains(medicalCase.getRequiredSpecialization());
//        if (!hasRequiredSpec) {
//            return false;
//        }
//
//        // Complexity check
//        if (medicalCase.getComplexity().ordinal() > doctor.getMaxComplexityLevel().ordinal()) {
//            return false;
//        }
//
//        // Urgency check
//        if ((medicalCase.getUrgencyLevel() == UrgencyLevel.CRITICAL ||
//             medicalCase.getUrgencyLevel() == UrgencyLevel.HIGH) &&
//            !doctor.getAcceptsUrgentCases()) {
//            return false;
//        }
//
//        // Second opinion check
//        if (medicalCase.getRequiresSecondOpinion() && !doctor.getAcceptsSecondOpinions()) {
//            return false;
//        }
//
//        // Check if doctor already has assignment for this case
//        boolean alreadyAssigned = caseAssignmentRepository.existsByCaseEntityIdAndDoctorId(
//            medicalCase.getId(), doctor.getDoctorId());
//        if (alreadyAssigned) {
//            return false;
//        }
//
//        return true;
//    }
//
//    /**
//     * Calculate matching scores for all eligible doctors
//     */
//    private List<DoctorMatchingResultDto> calculateMatchingScores(Case medicalCase, List<DoctorDto> eligibleDoctors) {
//        return eligibleDoctors.parallelStream()
//            .map(doctor -> calculateMatchingScore(medicalCase, doctor))
//            .filter(result -> result.getTotalScore() >= 30.0) // Minimum threshold
//            .collect(Collectors.toList());
//    }
//
//    /**
//     * Select the best doctors based on scoring and case requirements
//     */
//    private List<DoctorMatchingResultDto> selectBestDoctors(Case medicalCase, List<DoctorMatchingResultDto> matchingResults) {
//        // Sort by score descending
//        List<DoctorMatchingResultDto> sortedResults = matchingResults.stream()
//            .sorted((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()))
//            .collect(Collectors.toList());
//
//        List<DoctorMatchingResultDto> selectedDoctors = new ArrayList<>();
//
//        // Always try to assign primary doctor (highest score)
//        if (!sortedResults.isEmpty()) {
//            DoctorMatchingResultDto primary = sortedResults.get(0);
//            primary.setPriority(AssignmentPriority.PRIMARY);
//            selectedDoctors.add(primary);
//        }
//
//        // Select additional doctors for second opinions
//        int remainingSlots = Math.min(medicalCase.getMaxDoctorsAllowed() - 1,
//                                    medicalCase.getMinDoctorsRequired() - 1);
//
//        for (int i = 1; i < sortedResults.size() && selectedDoctors.size() < medicalCase.getMaxDoctorsAllowed(); i++) {
//            DoctorMatchingResultDto candidate = sortedResults.get(i);
//
//            // Ensure diversity in selections
//            if (isDiverseSelection(selectedDoctors, candidate)) {
//                candidate.setPriority(selectedDoctors.size() == 1 ?
//                    AssignmentPriority.SECONDARY : AssignmentPriority.CONSULTANT);
//                selectedDoctors.add(candidate);
//            }
//        }
//
//        // If we don't have minimum required doctors, add more (with lower threshold)
//        if (selectedDoctors.size() < medicalCase.getMinDoctorsRequired()) {
//            for (DoctorMatchingResultDto result : matchingResults) {
//                if (selectedDoctors.size() >= medicalCase.getMinDoctorsRequired()) break;
//
//                boolean alreadySelected = selectedDoctors.stream()
//                    .anyMatch(selected -> selected.getDoctor().getDoctorId().
//                            equals(result.getDoctor().getDoctorId()));
//
//                if (!alreadySelected && result.getTotalScore() >= 20.0) { // Lower threshold
//                    result.setPriority(AssignmentPriority.CONSULTANT);
//                    selectedDoctors.add(result);
//                }
//            }
//        }
//
//        return selectedDoctors;
//    }
//
//    /**
//     * Ensure diverse selection of doctors (different subspecializations if possible)
//     */
//    private boolean isDiverseSelection(List<DoctorMatchingResultDto> selected, DoctorMatchingResultDto candidate) {
//        if (selected.isEmpty()) return true;
//
//        // Check if candidate brings different subspecializations
//        Set<String> selectedSubSpecs = selected.stream()
//            .flatMap(result -> result.getDoctor().getSubSpecializationCodes().stream())
//            .collect(Collectors.toSet());
//
//        Set<String> candidateSubSpecs = candidate.getDoctor().getSubSpecializationCodes();
//
//        // If candidate has unique subspecializations, it adds diversity
//        return candidateSubSpecs.stream().anyMatch(spec -> !selectedSubSpecs.contains(spec)) ||
//               selected.size() < 2; // Always accept first two regardless
//    }
//
//    /**
//     * Create case assignments for selected doctors
//     */
//    private List<CaseAssignment> createCaseAssignments(Case medicalCase, List<DoctorMatchingResultDto> selectedDoctors) {
//        List<CaseAssignment> assignments = new ArrayList<>();
//        LocalDateTime now = LocalDateTime.now();
//
//        for (DoctorMatchingResultDto result : selectedDoctors) {
//            CaseAssignment assignment = CaseAssignment.builder()
//                .caseEntity(medicalCase)
//                .doctorId(result.getDoctor().getUserId())
//                .status(AssignmentStatus.PENDING)
//                .priority(result.getPriority())
//                .assignedAt(now)
//                .expiresAt(calculateExpirationTime(medicalCase.getUrgencyLevel()))
//                .assignmentReason(result.getMatchingReason())
//                .matchingScore(result.getTotalScore())
//                .build();
//
//            assignments.add(caseAssignmentRepository.save(assignment));
//
//            // Update doctor's current case load
//            try{
//                // 🔥 NEW: Send Kafka event instead of direct notification
//                patientEventProducer.sendCaseStatusUpdateEvent(
//                        assignment.getCaseEntity().getId(), "", "",
//                        assignment.getCaseEntity().getPatient().getUserId(),
//                        assignment.getDoctorId()
//                );
//            }catch (Exception e) {
//                log.error("Failed to update doctor work load.");
//                log.error(e.getMessage());
//            }
//        }
//        return assignments;
//    }
//
//    /**
//     * Calculate assignment expiration time based on urgency
//     */
//    private LocalDateTime calculateExpirationTime(UrgencyLevel urgency) {
//        LocalDateTime now = LocalDateTime.now();
//        return switch (urgency) {
//            case CRITICAL -> now.plusHours(2);   // 2 hours for critical
//            case HIGH -> now.plusHours(6);       // 6 hours for high
//            case MEDIUM -> now.plusHours(24);    // 24 hours for medium
//            case LOW -> now.plusHours(48);       // 48 hours for low
//        };
//    }
//
//    /**
//     * Update case after successful assignment
//     */
//    private void updateCaseAfterAssignment(Case medicalCase, List<CaseAssignment> assignments) {
//        medicalCase.setStatus(CaseStatus.ASSIGNED);
//        medicalCase.setFirstAssignedAt(LocalDateTime.now());
//        medicalCase.setLastAssignedAt(LocalDateTime.now());
//        medicalCase.setAssignmentAttempts(medicalCase.getAssignmentAttempts() + 1);
//        caseRepository.save(medicalCase);
//    }
//
//    private void sendAssignmentNotifications(Case medicalCase, List<CaseAssignment> assignments) {
//        for (CaseAssignment assignment : assignments) {
//            String title ="";
//            String message = "";
//            try {
//                title = String.format("New %s Case Assignment",
//                    assignment.getPriority().name().toLowerCase());
//                message = String.format(
//                    "You have been assigned a %s case: %s. Urgency: %s. Please review and respond by %s.",
//                    assignment.getPriority().name().toLowerCase(),
//                    medicalCase.getCaseTitle(),
//                    medicalCase.getUrgencyLevel().name(),
//                    assignment.getExpiresAt().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
//                );
//
//                patientEventProducer.sendAssignmentNotification(
//                        medicalCase.getPatient().getUserId(),
//                        assignment.getDoctorId(),
//                        medicalCase.getId(),
//                        title,
//                        message
//                        );
//            } catch (Exception e) {
//                log.error("Failed to send notification for assignment {}", assignment.getId(), e);
//            }
//        }
//    }
//
//    /**
//     * Generate human-readable matching reason
//     */
//    private String generateMatchingReason(Map<String, Double> scoreBreakdown,
//                                          Case medicalCase, DoctorDto doctor) {
//        StringBuilder reason = new StringBuilder();
//
//        reason.append("Matched based on: ");
//
//        List<String> strengths = new ArrayList<>();
//
//        if (scoreBreakdown.get("specialization") >= 70) {
//            strengths.add("Strong specialization match");
//        }
//        if (scoreBreakdown.get("disease_expertise") >= 60) {
//            strengths.add("Disease expertise");
//        }
//        if (scoreBreakdown.get("experience") >= 70) {
//            strengths.add("Extensive experience");
//        }
//        if (scoreBreakdown.get("availability") >= 80) {
//            strengths.add("High availability");
//        }
//        if (scoreBreakdown.get("performance") >= 80) {
//            strengths.add("Excellent performance history");
//        }
//
//        if (strengths.isEmpty()) {
//            reason.append("General qualification and availability");
//        } else {
//            reason.append(String.join(", ", strengths));
//        }
//
//        return reason.toString();
//    }
//
//    /**
//     * Validate that a case can be assigned
//     */
//    private void validateCaseForAssignment(Case medicalCase) {
//        if (medicalCase.getStatus() != CaseStatus.PENDING && medicalCase.getStatus() != CaseStatus.SUBMITTED) {
//            throw new BusinessException("Case is not in a state that allows assignment", HttpStatus.BAD_REQUEST);
//        }
//
//        // Check if patient has active subscription
//        Patient patient = medicalCase.getPatient();
//        if (patient.getSubscriptionStatus() != SubscriptionStatus.ACTIVE) {
//            throw new BusinessException("Patient must have active subscription", HttpStatus.PAYMENT_REQUIRED);
//        }
//
//        if (patient.getSubscriptionExpiry() != null &&
//            patient.getSubscriptionExpiry().isBefore(LocalDateTime.now())) {
//            throw new BusinessException("Patient subscription has expired", HttpStatus.PAYMENT_REQUIRED);
//        }
//    }
//
//    /**
//     * Handle case assignment expiration and reassignment
//     */
//    @Scheduled(fixedDelay = 300000) // Check every 5 minutes
//    public void handleExpiredAssignments() {
//        List<CaseAssignment> expiredAssignments = caseAssignmentRepository
//            .findByStatusAndExpiresAtBefore(AssignmentStatus.PENDING, LocalDateTime.now());
//
//        for (CaseAssignment assignment : expiredAssignments) {
//            assignment.setStatus(AssignmentStatus.EXPIRED);
//            caseAssignmentRepository.save(assignment);
//
//            try{
//                doctorServiceClient.updateDoctorLoad(assignment.getDoctorId(), CaseStatus.PENDING, 1);
//            }catch (Exception e) {
//                log.error("Failed to update doctor work load.");
//                log.error(e.getMessage());
//            }
//
//
//            // Try to reassign if case still needs doctors
//            Case medicalCase = assignment.getCaseEntity();
//            long activeAssignments = medicalCase.getAssignments().stream()
//                .filter(a -> a.getStatus() == AssignmentStatus.PENDING || a.getStatus() == AssignmentStatus.ACCEPTED)
//                .count();
//
//            if (activeAssignments < medicalCase.getMinDoctorsRequired()) {
//                try {
//                    assignCaseToMultipleDoctors(medicalCase.getId());
//                } catch (Exception e) {
//                    log.error("Failed to reassign case {} after expiration", medicalCase.getId(), e);
//                }
//            }
//        }
//    }
}
