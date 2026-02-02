package com.patientservice.service;

import com.commonlibrary.dto.*;
import com.patientservice.entity.Case;
import com.patientservice.entity.CaseAssignment;
import com.commonlibrary.entity.CaseStatus;
import com.commonlibrary.entity.UrgencyLevel;
import com.patientservice.repository.CaseAssignmentRepository;
import com.patientservice.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analytics methods for PatientAdminService
 * Comprehensive case analytics calculations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseAnalyticsService {

    private final CaseRepository caseRepository;
    private final CaseAssignmentRepository caseAssignmentRepository;

    /**
     * Main entry point for case analytics
     */
    public CaseAnalyticsDto getCaseAnalytics(String startDateStr, String endDateStr) {
        log.info("Calculating case analytics from {} to {}", startDateStr, endDateStr);
        
        try {
            // Parse dates or use defaults (last 360 days)
            LocalDateTime startDate = parseDate(startDateStr, LocalDateTime.now().minusYears(1));
            LocalDateTime endDate = parseDate(endDateStr, LocalDateTime.now());
            
            // Fetch data
            List<Case> cases = caseRepository.findBySubmittedAtBetweenAndIsDeletedFalse(startDate, endDate);
            //List<Long> caseIds = cases.stream().map(Case::getId).collect(Collectors.toList());
            List<CaseAssignment> assignments = cases.isEmpty() ?
                    new ArrayList<>() : 
                    caseAssignmentRepository.findByCaseEntityIn(cases);
            
            log.info("Found {} cases and {} assignments for analysis", cases.size(), assignments.size());
            
            // Calculate all metrics
            CaseAnalyticsDto analytics = CaseAnalyticsDto.builder()
                    .overview(calculateOverviewMetrics(cases, assignments))
                    .performance(calculatePerformanceMetrics(cases, assignments))
                    .doctorMetrics(calculateDoctorMetrics(assignments, cases))
                    .specializationMetrics(calculateSpecializationMetrics(cases))
                    .trends(calculateTrends(cases))
                    .qualityMetrics(calculateQualityMetrics(cases, assignments))
                    .startDate(startDate.toString())
                    .endDate(endDate.toString())
                    .totalCasesAnalyzed((long) cases.size())
                    .generatedAt(LocalDateTime.now().toString())
                    .build();
            
            log.info("Analytics calculation completed successfully");
            return analytics;
            
        } catch (Exception e) {
            log.error("Error calculating analytics: {}", e.getMessage(), e);
            // Return empty analytics rather than failing
            return createEmptyAnalytics();
        }
    }

    /**
     * Calculate overview metrics
     */
    private CaseOverviewMetrics calculateOverviewMetrics(List<Case> cases, List<CaseAssignment> assignments) {
        log.debug("Calculating overview metrics for {} cases", cases.size());
        
        long totalCases = cases.size();
        long activeCases = cases.stream()
                .filter(c -> !c.getStatus().equals(CaseStatus.CLOSED) && 
                            !c.getStatus().equals(CaseStatus.REJECTED))
                .count();
        long closedCases = cases.stream()
                .filter(c -> c.getStatus().equals(CaseStatus.CLOSED))
                .count();
        
        // Cases at risk (overdue based on urgency)
        long casesAtRisk = calculateCasesAtRisk(cases);
        
        // Assignment success rate
        double assignmentSuccessRate = calculateAssignmentSuccessRate(assignments);
        
        // Active doctors
        int activeDoctors = (int) assignments.stream()
                .map(CaseAssignment::getDoctorId)
                .distinct()
                .count();
        
        // Average times
        double avgAssignmentTime = calculateAverageAssignmentTime(cases);
        double avgResolutionTime = calculateAverageResolutionTime(cases);
        double avgResponseTime = calculateAverageResponseTime(assignments);
        
        // Distributions
        Map<String, Long> statusDistribution = cases.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getStatus().name(),
                        Collectors.counting()
                ));
        
        Map<String, Long> urgencyDistribution = cases.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getUrgencyLevel().name(),
                        Collectors.counting()
                ));
        
        return CaseOverviewMetrics.builder()
                .totalCases(totalCases)
                .activeCases(activeCases)
                .closedCases(closedCases)
                .casesAtRisk(casesAtRisk)
                .assignmentSuccessRate(round(assignmentSuccessRate, 2))
                .activeDoctorsCount(activeDoctors)
                .avgAssignmentTime(round(avgAssignmentTime, 2))
                .avgResolutionTime(round(avgResolutionTime, 2))
                .avgResponseTime(round(avgResponseTime, 2))
                .statusDistribution(statusDistribution)
                .urgencyDistribution(urgencyDistribution)
                .caseTrend(calculateCaseTrend(cases))
                .assignmentTimeTrend(calculateAssignmentTimeTrend(cases))
                .resolutionTimeTrend(calculateResolutionTimeTrend(cases))
                .build();
    }

    /**
     * Calculate cases at risk (overdue based on urgency SLA)
     */
    private long calculateCasesAtRisk(List<Case> cases) {
        LocalDateTime now = LocalDateTime.now();
        
        return cases.stream()
                .filter(c -> !c.getStatus().equals(CaseStatus.CLOSED) && 
                            !c.getStatus().equals(CaseStatus.REJECTED))
                .filter(c -> {
                    long hoursSinceSubmission = ChronoUnit.HOURS.between(c.getSubmittedAt(), now);
                    
                    // Check against SLA targets
                    switch (c.getUrgencyLevel()) {
                        case CRITICAL:
                            return hoursSinceSubmission > 1 && c.getFirstAssignedAt() == null;
                        case HIGH:
                            return hoursSinceSubmission > 4 && c.getFirstAssignedAt() == null;
                        case MEDIUM:
                            return hoursSinceSubmission > 24 && c.getFirstAssignedAt() == null;
                        case LOW:
                            return hoursSinceSubmission > 48 && c.getFirstAssignedAt() == null;
                        default:
                            return false;
                    }
                })
                .count();
    }

    /**
     * Calculate assignment success rate
     */
    private double calculateAssignmentSuccessRate(List<CaseAssignment> assignments) {
        if (assignments.isEmpty()) return 0.0;
        
        long successfulAssignments = assignments.stream()
                .filter(a -> "ACCEPTED".equals(a.getStatus()))
                .count();
        
        return ((double) successfulAssignments / assignments.size()) * 100;
    }

    /**
     * Calculate average assignment time (hours from submission to first assignment)
     */
    private double calculateAverageAssignmentTime(List<Case> cases) {
        List<Long> assignmentTimes = cases.stream()
                .filter(c -> c.getFirstAssignedAt() != null)
                .map(c -> ChronoUnit.HOURS.between(c.getSubmittedAt(), c.getFirstAssignedAt()))
                .collect(Collectors.toList());
        
        if (assignmentTimes.isEmpty()) return 0.0;
        
        return assignmentTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
    }

    /**
     * Calculate average resolution time (days from submission to closure)
     */
    private double calculateAverageResolutionTime(List<Case> cases) {
        List<Long> resolutionTimes = cases.stream()
                .filter(c -> c.getStatus().equals(CaseStatus.CLOSED) && c.getClosedAt() != null)
                .map(c -> ChronoUnit.DAYS.between(c.getSubmittedAt(), c.getClosedAt()))
                .collect(Collectors.toList());
        
        if (resolutionTimes.isEmpty()) return 0.0;
        
        return resolutionTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
    }

    /**
     * Calculate average response time (hours for doctor to respond)
     */
    private double calculateAverageResponseTime(List<CaseAssignment> assignments) {
        List<Long> responseTimes = assignments.stream()
                .filter(a -> a.getRespondedAt() != null)
                .map(a -> ChronoUnit.HOURS.between(a.getAssignedAt(), a.getRespondedAt()))
                .collect(Collectors.toList());
        
        if (responseTimes.isEmpty()) return 0.0;
        
        return responseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
    }

    /**
     * Helper method to round double values
     */
    private double round(double value, int places) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0.0;
        
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * Parse date string or return default
     */
    private LocalDateTime parseDate(String dateStr, LocalDateTime defaultDate) {
        if (dateStr == null || dateStr.isEmpty()) {
            return defaultDate;
        }
        
        try {
            return LocalDateTime.parse(dateStr);
        } catch (Exception e) {
            log.warn("Failed to parse date {}, using default", dateStr);
            return defaultDate;
        }
    }

    /**
     * Create empty analytics object for error cases
     */
    private CaseAnalyticsDto createEmptyAnalytics() {
        return CaseAnalyticsDto.builder()
                .overview(CaseOverviewMetrics.builder().build())
                .performance(CasePerformanceMetrics.builder().build())
                .doctorMetrics(DoctorAnalyticsMetrics.builder().build())
                .specializationMetrics(SpecializationAnalyticsMetrics.builder().build())
                .trends(TrendAnalyticsMetrics.builder().build())
                .qualityMetrics(QualityMetricsDto.builder().build())
                .totalCasesAnalyzed(0L)
                .generatedAt(LocalDateTime.now().toString())
                .build();
    }

    /**
     * Calculate case volume trend (comparing current period to previous period)
     */
    private CaseOverviewMetrics.TrendIndicator calculateCaseTrend(List<Case> cases) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMonthAgo = now.minusMonths(1);
        LocalDateTime twoMonthsAgo = now.minusMonths(2);

        // Count cases in current month vs previous month
        long currentMonthCases = cases.stream()
                .filter(c -> c.getSubmittedAt().isAfter(oneMonthAgo))
                .count();

        long previousMonthCases = cases.stream()
                .filter(c -> c.getSubmittedAt().isAfter(twoMonthsAgo) &&
                        c.getSubmittedAt().isBefore(oneMonthAgo))
                .count();

        // Calculate percentage change
        double changePercentage = 0.0;
        boolean isPositive = true;

        if (previousMonthCases > 0) {
            changePercentage = ((double) (currentMonthCases - previousMonthCases) / previousMonthCases) * 100;
            isPositive = changePercentage >= 0;
        } else if (currentMonthCases > 0) {
            changePercentage = 100.0;
            isPositive = true;
        }

        return CaseOverviewMetrics.TrendIndicator.builder()
                .value(Math.abs(round(changePercentage, 1)))
                .isPositive(isPositive)
                .period("vs last month")
                .build();
    }

    /**
     * Calculate assignment time trend (comparing current to previous period)
     */
    private CaseOverviewMetrics.TrendIndicator calculateAssignmentTimeTrend(List<Case> cases) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMonthAgo = now.minusMonths(1);
        LocalDateTime twoMonthsAgo = now.minusMonths(2);

        // Calculate average assignment time for current month
        double currentMonthAvgTime = cases.stream()
                .filter(c -> c.getSubmittedAt().isAfter(oneMonthAgo))
                .filter(c -> c.getFirstAssignedAt() != null)
                .mapToLong(c -> ChronoUnit.HOURS.between(c.getSubmittedAt(), c.getFirstAssignedAt()))
                .average()
                .orElse(0.0);

        // Calculate average assignment time for previous month
        double previousMonthAvgTime = cases.stream()
                .filter(c -> c.getSubmittedAt().isAfter(twoMonthsAgo) &&
                        c.getSubmittedAt().isBefore(oneMonthAgo))
                .filter(c -> c.getFirstAssignedAt() != null)
                .mapToLong(c -> ChronoUnit.HOURS.between(c.getSubmittedAt(), c.getFirstAssignedAt()))
                .average()
                .orElse(0.0);

        // Calculate percentage change (negative is good for time metrics)
        double changePercentage = 0.0;
        boolean isPositive = true; // For time, decrease is positive

        if (previousMonthAvgTime > 0) {
            changePercentage = ((currentMonthAvgTime - previousMonthAvgTime) / previousMonthAvgTime) * 100;
            isPositive = changePercentage <= 0; // Decrease in time is positive
        }

        return CaseOverviewMetrics.TrendIndicator.builder()
                .value(Math.abs(round(changePercentage, 1)))
                .isPositive(isPositive)
                .period("vs last month")
                .build();
    }

    /**
     * Calculate resolution time trend (comparing current to previous period)
     */
    private CaseOverviewMetrics.TrendIndicator calculateResolutionTimeTrend(List<Case> cases) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMonthAgo = now.minusMonths(1);
        LocalDateTime twoMonthsAgo = now.minusMonths(2);

        // Calculate average resolution time for current month
        double currentMonthAvgDays = cases.stream()
                .filter(c -> c.getSubmittedAt().isAfter(oneMonthAgo))
                .filter(c -> c.getStatus().equals(CaseStatus.CLOSED) && c.getClosedAt() != null)
                .mapToLong(c -> ChronoUnit.DAYS.between(c.getSubmittedAt(), c.getClosedAt()))
                .average()
                .orElse(0.0);

        // Calculate average resolution time for previous month
        double previousMonthAvgDays = cases.stream()
                .filter(c -> c.getSubmittedAt().isAfter(twoMonthsAgo) &&
                        c.getSubmittedAt().isBefore(oneMonthAgo))
                .filter(c -> c.getStatus().equals(CaseStatus.CLOSED) && c.getClosedAt() != null)
                .mapToLong(c -> ChronoUnit.DAYS.between(c.getSubmittedAt(), c.getClosedAt()))
                .average()
                .orElse(0.0);

        // Calculate percentage change (negative is good for time metrics)
        double changePercentage = 0.0;
        boolean isPositive = true; // For time, decrease is positive

        if (previousMonthAvgDays > 0) {
            changePercentage = ((currentMonthAvgDays - previousMonthAvgDays) / previousMonthAvgDays) * 100;
            isPositive = changePercentage <= 0; // Decrease in time is positive
        }

        return CaseOverviewMetrics.TrendIndicator.builder()
                .value(Math.abs(round(changePercentage, 1)))
                .isPositive(isPositive)
                .period("vs last month")
                .build();
    }

    /**
     * Calculate doctor analytics metrics
     */
    private DoctorAnalyticsMetrics calculateDoctorMetrics(List<CaseAssignment> assignments, List<Case> cases) {
        log.debug("Calculating doctor metrics for {} assignments", assignments.size());

        if (assignments.isEmpty()) {
            return DoctorAnalyticsMetrics.builder()
                    .topPerformers(new ArrayList<>())
                    .bottomPerformers(new ArrayList<>())
                    .workloadDistribution(new HashMap<>())
                    .avgAcceptanceRate(0.0)
                    .avgRejectionRate(0.0)
                    .totalActiveDoctors(0)
                    .totalAssignments(0L)
                    .casesByDoctor(new HashMap<>())
                    .avgCasesPerDoctor(0.0)
                    .utilization(DoctorAnalyticsMetrics.DoctorUtilization.builder()
                            .underutilizedCount(0)
                            .optimalCount(0)
                            .overutilizedCount(0)
                            .build())
                    .build();
        }

        // Group assignments by doctor
        Map<Long, List<CaseAssignment>> assignmentsByDoctor = assignments.stream()
                .collect(Collectors.groupingBy(CaseAssignment::getDoctorId));

        // Calculate performance for each doctor
        List<DoctorPerformanceDto> allPerformances = new ArrayList<>();

        for (Map.Entry<Long, List<CaseAssignment>> entry : assignmentsByDoctor.entrySet()) {
            Long doctorId = entry.getKey();
            List<CaseAssignment> doctorAssignments = entry.getValue();

            DoctorPerformanceDto performance = calculateDoctorPerformance(doctorId, doctorAssignments, cases);
            allPerformances.add(performance);
        }

        // Sort by acceptance rate descending for top performers
        List<DoctorPerformanceDto> topPerformers = allPerformances.stream()
                .sorted(Comparator.comparing(DoctorPerformanceDto::getAcceptanceRate).reversed())
                .limit(5)
                .collect(Collectors.toList());

        // Sort by acceptance rate ascending for bottom performers
        List<DoctorPerformanceDto> bottomPerformers = allPerformances.stream()
                .sorted(Comparator.comparing(DoctorPerformanceDto::getAcceptanceRate))
                .limit(5)
                .collect(Collectors.toList());

        // Calculate workload distribution
        Map<Long, Integer> workloadDistribution = new HashMap<>();
        Map<String, Integer> casesByDoctorName = new HashMap<>();

        for (DoctorPerformanceDto perf : allPerformances) {
            workloadDistribution.put(perf.getDoctorId(), perf.getCurrentLoad());
            casesByDoctorName.put(perf.getDoctorName(), perf.getTotalAssignments());
        }

        // Calculate average rates
        double avgAcceptanceRate = allPerformances.stream()
                .mapToDouble(DoctorPerformanceDto::getAcceptanceRate)
                .average()
                .orElse(0.0);

        double avgRejectionRate = allPerformances.stream()
                .mapToDouble(DoctorPerformanceDto::getRejectionRate)
                .average()
                .orElse(0.0);

        // Calculate utilization
        int underutilized = (int) allPerformances.stream()
                .filter(p -> p.getCurrentLoad() < 5)
                .count();

        int optimal = (int) allPerformances.stream()
                .filter(p -> p.getCurrentLoad() >= 5 && p.getCurrentLoad() <= 15)
                .count();

        int overutilized = (int) allPerformances.stream()
                .filter(p -> p.getCurrentLoad() > 15)
                .count();

        double avgCasesPerDoctor = allPerformances.stream()
                .mapToInt(DoctorPerformanceDto::getTotalAssignments)
                .average()
                .orElse(0.0);

        return DoctorAnalyticsMetrics.builder()
                .topPerformers(topPerformers)
                .bottomPerformers(bottomPerformers)
                .workloadDistribution(workloadDistribution)
                .avgAcceptanceRate(round(avgAcceptanceRate, 2))
                .avgRejectionRate(round(avgRejectionRate, 2))
                .totalActiveDoctors(allPerformances.size())
                .totalAssignments((long) assignments.size())
                .casesByDoctor(casesByDoctorName)
                .avgCasesPerDoctor(round(avgCasesPerDoctor, 2))
                .utilization(DoctorAnalyticsMetrics.DoctorUtilization.builder()
                        .underutilizedCount(underutilized)
                        .optimalCount(optimal)
                        .overutilizedCount(overutilized)
                        .build())
                .build();
    }

    /**
     * Calculate performance metrics for individual doctor
     */
    private DoctorPerformanceDto calculateDoctorPerformance(Long doctorId,
                                                            List<CaseAssignment> assignments,
                                                            List<Case> allCases) {

        int totalAssignments = assignments.size();
        int acceptedCases = (int) assignments.stream()
                .filter(a -> "ACCEPTED".equals(a.getStatus()))
                .count();
        int rejectedCases = (int) assignments.stream()
                .filter(a -> "REJECTED".equals(a.getStatus()))
                .count();

        // Get completed cases for this doctor
        Set<Case> assignedCaseIds = assignments.stream()
                .map(CaseAssignment::getCaseEntity)
                .collect(Collectors.toSet());

        int completedCases = (int) allCases.stream()
                .filter(c -> assignedCaseIds.contains(c.getId()))
                .filter(c -> c.getStatus().equals(CaseStatus.CLOSED))
                .count();

        // Current load (cases not closed)
        int currentLoad = (int) allCases.stream()
                .filter(c -> assignedCaseIds.contains(c.getId()))
                .filter(c -> !c.getStatus().equals(CaseStatus.CLOSED))
                .count();

        // Calculate rates
        double acceptanceRate = totalAssignments > 0 ?
                ((double) acceptedCases / totalAssignments) * 100 : 0.0;
        double rejectionRate = totalAssignments > 0 ?
                ((double) rejectedCases / totalAssignments) * 100 : 0.0;

        // Average resolution time
        double avgResolutionTime = allCases.stream()
                .filter(c -> assignedCaseIds.contains(c.getId()))
                .filter(c -> c.getStatus().equals(CaseStatus.CLOSED) && c.getClosedAt() != null)
                .mapToLong(c -> ChronoUnit.DAYS.between(c.getSubmittedAt(), c.getClosedAt()))
                .average()
                .orElse(0.0);

        // Average response time
        double avgResponseTime = assignments.stream()
                .filter(a -> a.getRespondedAt() != null)
                .mapToLong(a -> ChronoUnit.HOURS.between(a.getAssignedAt(), a.getRespondedAt()))
                .average()
                .orElse(0.0);

        // Reassignment count
        int reassignmentCount = (int) assignments.stream()
                .filter(a -> a.getAssignedAt() != null)
                .count();

        // Determine performance level
        String performanceLevel = determinePerformanceLevel(acceptanceRate, avgResolutionTime);

        // Get doctor name (you may need to fetch from doctor service or use a map)
        String doctorName = "Doctor " + doctorId; // TODO: Fetch actual name from DoctorService
        String specialization = "Unknown"; // TODO: Fetch from DoctorService

        return DoctorPerformanceDto.builder()
                .doctorId(doctorId)
                .doctorName(doctorName)
                .specialization(specialization)
                .totalAssignments(totalAssignments)
                .acceptedCases(acceptedCases)
                .rejectedCases(rejectedCases)
                .completedCases(completedCases)
                .currentLoad(currentLoad)
                .acceptanceRate(round(acceptanceRate, 2))
                .rejectionRate(round(rejectionRate, 2))
                .avgResolutionTime(round(avgResolutionTime, 2))
                .avgResponseTime(round(avgResponseTime, 2))
                .reassignmentCount(reassignmentCount)
                .patientSatisfaction(0.0) // TODO: Implement if ratings available
                .performanceLevel(performanceLevel)
                .build();
    }

    /**
     * Determine performance level based on metrics
     */
    private String determinePerformanceLevel(double acceptanceRate, double avgResolutionTime) {
        if (acceptanceRate >= 90 && avgResolutionTime <= 3) {
            return "Excellent";
        } else if (acceptanceRate >= 75 && avgResolutionTime <= 5) {
            return "Good";
        } else if (acceptanceRate >= 60 && avgResolutionTime <= 7) {
            return "Average";
        } else {
            return "Needs Improvement";
        }
    }

    /**
     * Calculate specialization analytics
     */
    private SpecializationAnalyticsMetrics calculateSpecializationMetrics(List<Case> cases) {
        log.debug("Calculating specialization metrics for {} cases", cases.size());

        if (cases.isEmpty()) {
            return SpecializationAnalyticsMetrics.builder()
                    .casesBySpecialization(new HashMap<>())
                    .avgResolutionBySpecialization(new HashMap<>())
                    .avgFeeBySpecialization(new HashMap<>())
                    .specializationTrends(new ArrayList<>())
                    .distributionPercentage(new HashMap<>())
                    .totalSpecializations(0)
                    .build();
        }

        // Count cases by specialization
        Map<String, Long> casesBySpec = cases.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getRequiredSpecialization() != null ? c.getRequiredSpecialization() : "GENERAL",
                        Collectors.counting()
                ));

        // Average resolution time by specialization
        Map<String, Double> avgResolutionBySpec = new HashMap<>();
        for (String spec : casesBySpec.keySet()) {
            double avgResolution = cases.stream()
                    .filter(c -> spec.equals(c.getRequiredSpecialization()))
                    .filter(c -> c.getStatus().equals(CaseStatus.CLOSED) && c.getClosedAt() != null)
                    .mapToLong(c -> ChronoUnit.DAYS.between(c.getSubmittedAt(), c.getClosedAt()))
                    .average()
                    .orElse(0.0);

            avgResolutionBySpec.put(spec, round(avgResolution, 2));
        }

        // Average fee by specialization
        Map<String, BigDecimal> avgFeeBySpec = new HashMap<>();
        for (String spec : casesBySpec.keySet()) {
            BigDecimal avgFee = cases.stream()
                    .filter(c -> spec.equals(c.getRequiredSpecialization()))
                    .filter(c -> c.getConsultationFee() != null)
                    .map(Case::getConsultationFee)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(casesBySpec.get(spec)), 2, RoundingMode.HALF_UP);

            avgFeeBySpec.put(spec, avgFee);
        }

        // Calculate trends
        List<SpecializationTrendDto> trends = calculateSpecializationTrends(cases, casesBySpec);

        // Distribution percentage
        long totalCases = cases.size();
        Map<String, Double> distributionPercentage = casesBySpec.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> round(((double) e.getValue() / totalCases) * 100, 2)
                ));

        // Find insights
        String mostInDemand = casesBySpec.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        String fastestResolution = avgResolutionBySpec.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        String highestFee = avgFeeBySpec.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        String fastestGrowth = trends.stream()
                .filter(t -> t.getMonthlyGrowthRate() != null && t.getMonthlyGrowthRate() > 0)
                .max(Comparator.comparing(SpecializationTrendDto::getMonthlyGrowthRate))
                .map(SpecializationTrendDto::getSpecialization)
                .orElse("N/A");

        return SpecializationAnalyticsMetrics.builder()
                .casesBySpecialization(casesBySpec)
                .avgResolutionBySpecialization(avgResolutionBySpec)
                .avgFeeBySpecialization(avgFeeBySpec)
                .specializationTrends(trends)
                .mostInDemand(mostInDemand)
                .fastestResolution(fastestResolution)
                .highestFee(highestFee)
                .fastestGrowth(fastestGrowth)
                .distributionPercentage(distributionPercentage)
                .totalSpecializations(casesBySpec.size())
                .build();
    }

    /**
     * Calculate specialization trends
     */
    private List<SpecializationTrendDto> calculateSpecializationTrends(List<Case> cases,
                                                                       Map<String, Long> casesBySpec) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMonthAgo = now.minusMonths(1);
        LocalDateTime twoMonthsAgo = now.minusMonths(2);
        LocalDateTime oneWeekAgo = now.minusWeeks(1);
        LocalDateTime twoWeeksAgo = now.minusWeeks(2);

        List<SpecializationTrendDto> trends = new ArrayList<>();

        for (String spec : casesBySpec.keySet()) {
            // Current month
            long currentMonth = cases.stream()
                    .filter(c -> spec.equals(c.getRequiredSpecialization()))
                    .filter(c -> c.getSubmittedAt().isAfter(oneMonthAgo))
                    .count();

            // Previous month
            long previousMonth = cases.stream()
                    .filter(c -> spec.equals(c.getRequiredSpecialization()))
                    .filter(c -> c.getSubmittedAt().isAfter(twoMonthsAgo) &&
                            c.getSubmittedAt().isBefore(oneMonthAgo))
                    .count();

            // Current week
            long currentWeek = cases.stream()
                    .filter(c -> spec.equals(c.getRequiredSpecialization()))
                    .filter(c -> c.getSubmittedAt().isAfter(oneWeekAgo))
                    .count();

            // Previous week
            long previousWeek = cases.stream()
                    .filter(c -> spec.equals(c.getRequiredSpecialization()))
                    .filter(c -> c.getSubmittedAt().isAfter(twoWeeksAgo) &&
                            c.getSubmittedAt().isBefore(oneWeekAgo))
                    .count();

            // Calculate growth rates
            double monthlyGrowth = previousMonth > 0 ?
                    ((double) (currentMonth - previousMonth) / previousMonth) * 100 : 0.0;

            double weeklyGrowth = previousWeek > 0 ?
                    ((double) (currentWeek - previousWeek) / previousWeek) * 100 : 0.0;

            String trendDirection = monthlyGrowth > 5 ? "UP" :
                    monthlyGrowth < -5 ? "DOWN" : "STABLE";

            trends.add(SpecializationTrendDto.builder()
                    .specialization(spec)
                    .currentMonthCases(currentMonth)
                    .previousMonthCases(previousMonth)
                    .currentWeekCases(currentWeek)
                    .previousWeekCases(previousWeek)
                    .monthlyGrowthRate(round(monthlyGrowth, 2))
                    .weeklyGrowthRate(round(weeklyGrowth, 2))
                    .trendDirection(trendDirection)
                    .isGrowing(monthlyGrowth > 0)
                    .monthlyChange(currentMonth - previousMonth)
                    .weeklyChange(currentWeek - previousWeek)
                    .build());
        }

        return trends;
    }

    /**
     * Calculate trend analytics
     */
    private TrendAnalyticsMetrics calculateTrends(List<Case> cases) {
        log.debug("Calculating trend metrics for {} cases", cases.size());

        if (cases.isEmpty()) {
            return TrendAnalyticsMetrics.builder()
                    .dailyTrend(new ArrayList<>())
                    .weeklyTrend(new ArrayList<>())
                    .monthlyTrend(new ArrayList<>())
                    .hourlyDistribution(new HashMap<>())
                    .dayOfWeekDistribution(new HashMap<>())
                    .statusTrend(new ArrayList<>())
                    .build();
        }

        return TrendAnalyticsMetrics.builder()
                .dailyTrend(getDailyTrend(cases))
                .weeklyTrend(getWeeklyTrend(cases))
                .monthlyTrend(getMonthlyTrend(cases))
                .hourlyDistribution(getHourlyDistribution(cases))
                .dayOfWeekDistribution(getDayOfWeekDistribution(cases))
                .statusTrend(getStatusTrend(cases))
                .peakHour(findPeakHour(cases))
                .peakDay(findPeakDay(cases))
                .peakMonth(findPeakMonth(cases))
                .weekOverWeekGrowth(calculateWeekOverWeekGrowth(cases))
                .monthOverMonthGrowth(calculateMonthOverMonthGrowth(cases))
                .yearOverYearGrowth(calculateYearOverYearGrowth(cases))
                .build();
    }

    /**
     * Get daily trend (last 30 days)
     */
    private List<ChartDataPointDto> getDailyTrend(List<Case> cases) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);

        Map<LocalDate, List<Case>> casesByDay = cases.stream()
                .filter(c -> c.getSubmittedAt().isAfter(thirtyDaysAgo))
                .collect(Collectors.groupingBy(c -> c.getSubmittedAt().toLocalDate()));

        List<ChartDataPointDto> dailyData = new ArrayList<>();

        for (int i = 30; i >= 0; i--) {
            LocalDate date = now.minusDays(i).toLocalDate();
            List<Case> dayCases = casesByDay.getOrDefault(date, new ArrayList<>());

            dailyData.add(ChartDataPointDto.builder()
                    .date(date.toString())
                    .label(date.format(DateTimeFormatter.ofPattern("MMM dd")))
                    .earnings(BigDecimal.valueOf(dayCases.size()))
                    .count(dayCases.size())
                    .build());
        }

        return dailyData;
    }

    /**
     * Get weekly trend (last 12 weeks)
     */
    private List<ChartDataPointDto> getWeeklyTrend(List<Case> cases) {
        LocalDateTime now = LocalDateTime.now();

        List<ChartDataPointDto> weeklyData = new ArrayList<>();

        for (int i = 12; i >= 0; i--) {
            LocalDateTime weekStart = now.minusWeeks(i).with(DayOfWeek.MONDAY);
            LocalDateTime weekEnd = weekStart.plusWeeks(1);

            long weekCases = cases.stream()
                    .filter(c -> c.getSubmittedAt().isAfter(weekStart) &&
                            c.getSubmittedAt().isBefore(weekEnd))
                    .count();

            weeklyData.add(ChartDataPointDto.builder()
                    .date(weekStart.toLocalDate().toString())
                    .label("Week " + (13 - i))
                    .earnings(BigDecimal.valueOf(weekCases))
                    .count((int) weekCases)
                    .build());
        }

        return weeklyData;
    }

    /**
     * Get monthly trend (last 12 months)
     */
    private List<ChartDataPointDto> getMonthlyTrend(List<Case> cases) {
        LocalDateTime now = LocalDateTime.now();

        List<ChartDataPointDto> monthlyData = new ArrayList<>();

        for (int i = 12; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1);
            LocalDateTime monthEnd = monthStart.plusMonths(1);

            long monthCases = cases.stream()
                    .filter(c -> c.getSubmittedAt().isAfter(monthStart) &&
                            c.getSubmittedAt().isBefore(monthEnd))
                    .count();

            monthlyData.add(ChartDataPointDto.builder()
                    .date(monthStart.toLocalDate().toString())
                    .label(monthStart.format(DateTimeFormatter.ofPattern("MMM yyyy")))
                    .earnings(BigDecimal.valueOf(monthCases))
                    .count((int) monthCases)
                    .build());
        }

        return monthlyData;
    }

    /**
     * Get hourly distribution (0-23)
     */
    private Map<Integer, Integer> getHourlyDistribution(List<Case> cases) {
        Map<Integer, Integer> hourlyDist = new HashMap<>();

        // Initialize all hours
        for (int i = 0; i < 24; i++) {
            hourlyDist.put(i, 0);
        }

        // Count cases by hour
        cases.forEach(c -> {
            int hour = c.getSubmittedAt().getHour();
            hourlyDist.put(hour, hourlyDist.get(hour) + 1);
        });

        return hourlyDist;
    }

    /**
     * Get day of week distribution
     */
    private Map<String, Integer> getDayOfWeekDistribution(List<Case> cases) {
        Map<String, Integer> dayDist = new LinkedHashMap<>();

        // Initialize all days
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        for (String day : days) {
            dayDist.put(day, 0);
        }

        // Count cases by day
        cases.forEach(c -> {
            String day = c.getSubmittedAt().getDayOfWeek().toString();
            day = day.substring(0, 1).toUpperCase() + day.substring(1).toLowerCase();
            dayDist.put(day, dayDist.getOrDefault(day, 0) + 1);
        });

        return dayDist;
    }

    /**
     * Get status trend over time
     */
    private List<TrendAnalyticsMetrics.StatusTrendPoint> getStatusTrend(List<Case> cases) {
        // Group by date and status
        Map<LocalDate, Map<String, Long>> trendData = cases.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getSubmittedAt().toLocalDate(),
                        Collectors.groupingBy(
                                c -> c.getStatus().name(),
                                Collectors.counting()
                        )
                ));

        List<TrendAnalyticsMetrics.StatusTrendPoint> trendPoints = new ArrayList<>();

        for (Map.Entry<LocalDate, Map<String, Long>> entry : trendData.entrySet()) {
            for (Map.Entry<String, Long> statusEntry : entry.getValue().entrySet()) {
                trendPoints.add(TrendAnalyticsMetrics.StatusTrendPoint.builder()
                        .date(entry.getKey().toString())
                        .status(statusEntry.getKey())
                        .count(statusEntry.getValue())
                        .build());
            }
        }

        return trendPoints;
    }

    /**
     * Find peak hour (most submissions)
     */
    private Integer findPeakHour(List<Case> cases) {
        return cases.stream()
                .collect(Collectors.groupingBy(c -> c.getSubmittedAt().getHour(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(12);
    }

    /**
     * Find peak day
     */
    private String findPeakDay(List<Case> cases) {
        return cases.stream()
                .collect(Collectors.groupingBy(c -> c.getSubmittedAt().getDayOfWeek(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().toString())
                .orElse("MONDAY");
    }

    /**
     * Find peak month
     */
    private String findPeakMonth(List<Case> cases) {
        return cases.stream()
                .collect(Collectors.groupingBy(c -> c.getSubmittedAt().getMonth(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().toString())
                .orElse("JANUARY");
    }

    /**
     * Calculate week-over-week growth
     */
    private Double calculateWeekOverWeekGrowth(List<Case> cases) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneWeekAgo = now.minusWeeks(1);
        LocalDateTime twoWeeksAgo = now.minusWeeks(2);

        long thisWeek = cases.stream()
                .filter(c -> c.getSubmittedAt().isAfter(oneWeekAgo))
                .count();

        long lastWeek = cases.stream()
                .filter(c -> c.getSubmittedAt().isAfter(twoWeeksAgo) &&
                        c.getSubmittedAt().isBefore(oneWeekAgo))
                .count();

        if (lastWeek == 0) return 0.0;

        return round(((double) (thisWeek - lastWeek) / lastWeek) * 100, 2);
    }

    /**
     * Calculate month-over-month growth
     */
    private Double calculateMonthOverMonthGrowth(List<Case> cases) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMonthAgo = now.minusMonths(1);
        LocalDateTime twoMonthsAgo = now.minusMonths(2);

        long thisMonth = cases.stream()
                .filter(c -> c.getSubmittedAt().isAfter(oneMonthAgo))
                .count();

        long lastMonth = cases.stream()
                .filter(c -> c.getSubmittedAt().isAfter(twoMonthsAgo) &&
                        c.getSubmittedAt().isBefore(oneMonthAgo))
                .count();

        if (lastMonth == 0) return 0.0;

        return round(((double) (thisMonth - lastMonth) / lastMonth) * 100, 2);
    }

    /**
     * Calculate year-over-year growth
     */
    private Double calculateYearOverYearGrowth(List<Case> cases) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneYearAgo = now.minusYears(1);
        LocalDateTime twoYearsAgo = now.minusYears(2);

        long thisYear = cases.stream()
                .filter(c -> c.getSubmittedAt().isAfter(oneYearAgo))
                .count();

        long lastYear = cases.stream()
                .filter(c -> c.getSubmittedAt().isAfter(twoYearsAgo) &&
                        c.getSubmittedAt().isBefore(oneYearAgo))
                .count();

        if (lastYear == 0) return 0.0;

        return round(((double) (thisYear - lastYear) / lastYear) * 100, 2);
    }

    /**
     * Calculate quality metrics
     */
    private QualityMetricsDto calculateQualityMetrics(List<Case> cases, List<CaseAssignment> assignments) {
        log.debug("Calculating quality metrics");

        if (cases.isEmpty() || assignments.isEmpty()) {
            return QualityMetricsDto.builder()
                    .reassignmentRate(0.0)
                    .totalReassignments(0L)
                    .rejectionRate(0.0)
                    .totalRejections(0L)
                    .completionRate(0.0)
                    .completedCases(0L)
                    .abandonedCases(0L)
                    .avgDocumentationScore(0.0)
                    .casesWithCompleteInfo(0L)
                    .casesWithIncompleteInfo(0L)
                    .avgIterationsPerCase(0.0)
                    .firstTimeSuccessRate(0.0)
                    .avgTimeToFirstResponse(0.0)
                    .casesWithMultipleReassignments(0L)
                    .casesWithLongResponseTime(0L)
                    .casesWithIncompleteDocumentation(0L)
                    .build();
        }

        long totalAssignments = assignments.size();

        // Reassignment metrics
        long totalReassignments = assignments.stream()
                .filter(a -> a.getAssignedAt() != null)
                .count();
        double reassignmentRate = ((double) totalReassignments / totalAssignments) * 100;

        // Rejection metrics
        long totalRejections = assignments.stream()
                .filter(a -> "REJECTED".equals(a.getStatus()))
                .count();
        double rejectionRate = ((double) totalRejections / totalAssignments) * 100;

        // Completion metrics
        long completedCases = cases.stream()
                .filter(c -> c.getStatus().equals(CaseStatus.CLOSED))
                .count();
        long abandonedCases = cases.stream()
                .filter(c -> c.getStatus().equals(CaseStatus.REJECTED))
                .count();
        double completionRate = ((double) completedCases / cases.size()) * 100;

        // Documentation quality
        long casesWithCompleteInfo = cases.stream()
                .filter(c -> c.getSymptomCodes() != null && !c.getSymptomCodes().isEmpty() &&
                        c.getMedicalReportFileLink() != null && !c.getMedicalReportFileLink().isEmpty())
                .count();
        long casesWithIncompleteInfo = cases.size() - casesWithCompleteInfo;
        double avgDocScore = ((double) casesWithCompleteInfo / cases.size()) * 100;

        // First-time success rate
        Set<Long> firstAssignmentCaseIds = new HashSet<>();
        Set<Long> acceptedFirstTimeCaseIds = new HashSet<>();

        for (CaseAssignment assignment : assignments) {
            if (assignment.getAssignedAt() == null) {
                firstAssignmentCaseIds.add(assignment.getCaseEntity().getId());
                if ("ACCEPTED".equals(assignment.getStatus())) {
                    acceptedFirstTimeCaseIds.add(assignment.getCaseEntity().getId());
                }
            }
        }

        double firstTimeSuccess = firstAssignmentCaseIds.isEmpty() ? 0.0 :
                ((double) acceptedFirstTimeCaseIds.size() / firstAssignmentCaseIds.size()) * 100;

        // Average time to first response
        double avgTimeToFirstResponse = assignments.stream()
                .filter(a -> a.getRespondedAt() != null)
                .mapToLong(a -> ChronoUnit.HOURS.between(a.getAssignedAt(), a.getRespondedAt()))
                .average()
                .orElse(0.0);

        // Cases with multiple reassignments
        Map<Case, Long> reassignmentCounts = assignments.stream()
                .filter(a -> a.getAssignedAt() != null)
                .collect(Collectors.groupingBy(CaseAssignment::getCaseEntity, Collectors.counting()));

        long multipleReassignments = reassignmentCounts.values().stream()
                .filter(count -> count >= 2)
                .count();

        // Cases with long response time (>24 hours)
        long longResponseTime = assignments.stream()
                .filter(a -> a.getRespondedAt() != null)
                .filter(a -> ChronoUnit.HOURS.between(a.getAssignedAt(), a.getRespondedAt()) > 24)
                .count();

        return QualityMetricsDto.builder()
                .reassignmentRate(round(reassignmentRate, 2))
                .totalReassignments(totalReassignments)
                .rejectionRate(round(rejectionRate, 2))
                .totalRejections(totalRejections)
                .completionRate(round(completionRate, 2))
                .completedCases(completedCases)
                .abandonedCases(abandonedCases)
                .avgDocumentationScore(round(avgDocScore, 2))
                .casesWithCompleteInfo(casesWithCompleteInfo)
                .casesWithIncompleteInfo(casesWithIncompleteInfo)
                .avgIterationsPerCase(0.0) // TODO: Implement based on status history
                .maxIterations(0)
                .minIterations(0)
                .firstTimeSuccessRate(round(firstTimeSuccess, 2))
                .avgTimeToFirstResponse(round(avgTimeToFirstResponse, 2))
                .casesWithMultipleReassignments(multipleReassignments)
                .casesWithLongResponseTime(longResponseTime)
                .casesWithIncompleteDocumentation(casesWithIncompleteInfo)
                .qualityTrend(QualityMetricsDto.QualityTrend.builder()
                        .direction("STABLE")
                        .changePercentage(0.0)
                        .period("vs last month")
                        .build())
                .build();
    }

    /**
     * Calculate performance metrics
     */
    private CasePerformanceMetrics calculatePerformanceMetrics(List<Case> cases, List<CaseAssignment> assignments) {
        log.debug("Calculating performance metrics");

        // Average time by status
        Map<String, Double> avgTimeByStatus = calculateAvgTimeByStatus(cases);

        // Bottleneck analysis
        Map<String, Long> bottlenecks = calculateBottlenecks(cases);

        // SLA compliance
        SlaComplianceDto slaCompliance = calculateSlaCompliance(cases, assignments);

        // Stage funnel
        List<CaseStageMetrics> stageFunnel = calculateStageFunnel(cases);

        // Performance by urgency
        Map<String, CasePerformanceMetrics.UrgencyPerformance> performanceByUrgency =
                calculatePerformanceByUrgency(cases, assignments);

        return CasePerformanceMetrics.builder()
                .avgTimeByStatus(avgTimeByStatus)
                .bottleneckAnalysis(bottlenecks)
                .slaCompliance(slaCompliance)
                .stageFunnel(stageFunnel)
                .performanceByUrgency(performanceByUrgency)
                .build();
    }

    /**
     * Calculate average time spent in each status
     */
    private Map<String, Double> calculateAvgTimeByStatus(List<Case> cases) {
        Map<String, List<Long>> timesByStatus = new HashMap<>();

        for (Case c : cases) {
            CaseStatus status = c.getStatus();

            // Calculate time in current status
            LocalDateTime statusStartTime = getStatusStartTime(c, status);
            if (statusStartTime != null) {
                LocalDateTime endTime = status.equals(CaseStatus.CLOSED) && c.getClosedAt() != null ?
                        c.getClosedAt() : LocalDateTime.now();

                long hours = ChronoUnit.HOURS.between(statusStartTime, endTime);

                timesByStatus.computeIfAbsent(status.name(), k -> new ArrayList<>()).add(hours);
            }
        }

        // Calculate averages
        Map<String, Double> avgTimes = new HashMap<>();
        for (Map.Entry<String, List<Long>> entry : timesByStatus.entrySet()) {
            double avg = entry.getValue().stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);
            avgTimes.put(entry.getKey(), round(avg, 2));
        }

        return avgTimes;
    }

    /**
     * Get start time for a given status (approximation)
     */
    private LocalDateTime getStatusStartTime(Case c, CaseStatus status) {
        switch (status) {
            case SUBMITTED:
            case PENDING:
                return c.getSubmittedAt();
            case ASSIGNED:
            case ACCEPTED:
            case SCHEDULED:
            case IN_PROGRESS:
                return c.getFirstAssignedAt();
            case CLOSED:
                return c.getClosedAt() != null ? c.getClosedAt().minusDays(3) : null; // Estimate
            default:
                return c.getSubmittedAt();
        }
    }

    /**
     * Calculate bottlenecks - cases stuck in each status
     */
    private Map<String, Long> calculateBottlenecks(List<Case> cases) {
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);

        return cases.stream()
                .filter(c -> c.getSubmittedAt().isBefore(threeDaysAgo))
                .filter(c -> !c.getStatus().equals(CaseStatus.CLOSED))
                .collect(Collectors.groupingBy(
                        c -> c.getStatus().name(),
                        Collectors.counting()
                ));
    }

    /**
     * Calculate SLA compliance
     */
    private SlaComplianceDto calculateSlaCompliance(List<Case> cases, List<CaseAssignment> assignments) {
        Map<UrgencyLevel, List<Case>> casesByUrgency = cases.stream()
                .collect(Collectors.groupingBy(Case::getUrgencyLevel));

        double criticalCompliance = calculateUrgencySlaCompliance(
                casesByUrgency.getOrDefault(UrgencyLevel.CRITICAL, new ArrayList<>()), 1);
        double highCompliance = calculateUrgencySlaCompliance(
                casesByUrgency.getOrDefault(UrgencyLevel.HIGH, new ArrayList<>()), 4);
        double mediumCompliance = calculateUrgencySlaCompliance(
                casesByUrgency.getOrDefault(UrgencyLevel.MEDIUM, new ArrayList<>()), 24);
        double lowCompliance = calculateUrgencySlaCompliance(
                casesByUrgency.getOrDefault(UrgencyLevel.LOW, new ArrayList<>()), 48);

        long totalCases = cases.size();
        long casesMetSla = (long) ((criticalCompliance + highCompliance + mediumCompliance + lowCompliance) / 4 * totalCases / 100);

        return SlaComplianceDto.builder()
                .criticalTarget(1)
                .highTarget(4)
                .mediumTarget(24)
                .lowTarget(48)
                .criticalCompliance(round(criticalCompliance, 2))
                .highCompliance(round(highCompliance, 2))
                .mediumCompliance(round(mediumCompliance, 2))
                .lowCompliance(round(lowCompliance, 2))
                .overallCompliance(round((criticalCompliance + highCompliance + mediumCompliance + lowCompliance) / 4, 2))
                .totalCases(totalCases)
                .casesMetSla(casesMetSla)
                .casesMissedSla(totalCases - casesMetSla)
                .criticalAvgTime(round(calculateAvgAssignmentTimeByUrgency(casesByUrgency.getOrDefault(UrgencyLevel.CRITICAL, new ArrayList<>())), 2))
                .highAvgTime(round(calculateAvgAssignmentTimeByUrgency(casesByUrgency.getOrDefault(UrgencyLevel.HIGH, new ArrayList<>())), 2))
                .mediumAvgTime(round(calculateAvgAssignmentTimeByUrgency(casesByUrgency.getOrDefault(UrgencyLevel.MEDIUM, new ArrayList<>())), 2))
                .lowAvgTime(round(calculateAvgAssignmentTimeByUrgency(casesByUrgency.getOrDefault(UrgencyLevel.LOW, new ArrayList<>())), 2))
                .build();
    }

    /**
     * Calculate SLA compliance for specific urgency level
     */
    private double calculateUrgencySlaCompliance(List<Case> cases, int targetHours) {
        if (cases.isEmpty()) return 100.0;

        long metSla = cases.stream()
                .filter(c -> c.getFirstAssignedAt() != null)
                .filter(c -> {
                    long hours = ChronoUnit.HOURS.between(c.getSubmittedAt(), c.getFirstAssignedAt());
                    return hours <= targetHours;
                })
                .count();

        return ((double) metSla / cases.size()) * 100;
    }

    /**
     * Calculate average assignment time for specific urgency
     */
    private double calculateAvgAssignmentTimeByUrgency(List<Case> cases) {
        return cases.stream()
                .filter(c -> c.getFirstAssignedAt() != null)
                .mapToLong(c -> ChronoUnit.HOURS.between(c.getSubmittedAt(), c.getFirstAssignedAt()))
                .average()
                .orElse(0.0);
    }

    /**
     * Calculate stage funnel metrics
     */
    private List<CaseStageMetrics> calculateStageFunnel(List<Case> cases) {
        List<CaseStageMetrics> funnel = new ArrayList<>();

        // Define stages in order
        Map<String, Integer> stageOrder = Map.of(
                "SUBMITTED", 1,
                "PENDING", 2,
                "ASSIGNED", 3,
                "ACCEPTED", 4,
                "SCHEDULED", 5,
                "IN_PROGRESS", 6,
                "CONSULTATION_COMPLETE", 7,
                "CLOSED", 8
        );

        long totalCases = cases.size();

        for (Map.Entry<String, Integer> stage : stageOrder.entrySet()) {
            String stageName = stage.getKey();

            long casesInStage = cases.stream()
                    .filter(c -> c.getStatus().name().equals(stageName))
                    .count();

            long casesReachedStage = cases.stream()
                    .filter(c -> hasReachedStage(c, stageName))
                    .count();

            double reachRate = totalCases > 0 ? ((double) casesReachedStage / totalCases) * 100 : 0.0;

            funnel.add(CaseStageMetrics.builder()
                    .stageName(stageName)
                    .stageLabel(formatStageLabel(stageName))
                    .caseCount(casesInStage)
                    .dropoffCount(0L) // TODO: Calculate actual dropoff
                    .dropoffRate(0.0) // TODO: Calculate actual dropoff rate
                    .avgDuration(0.0) // TODO: Calculate from status history
                    .reachRate(round(reachRate, 2))
                    .stageOrder(stage.getValue())
                    .build());
        }

        return funnel;
    }

    /**
     * Check if case has reached a stage
     */
    private boolean hasReachedStage(Case c, String stageName) {
        // Simplified - in production, check status history
        CaseStatus currentStatus = c.getStatus();
        CaseStatus targetStatus = CaseStatus.valueOf(stageName);

        return currentStatus.ordinal() >= targetStatus.ordinal();
    }

    /**
     * Format stage label for display
     */
    private String formatStageLabel(String stageName) {
        return Arrays.stream(stageName.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    /**
     * Calculate performance by urgency level
     */
    private Map<String, CasePerformanceMetrics.UrgencyPerformance> calculatePerformanceByUrgency(
            List<Case> cases, List<CaseAssignment> assignments) {

        Map<String, CasePerformanceMetrics.UrgencyPerformance> performance = new HashMap<>();

        for (UrgencyLevel urgency : UrgencyLevel.values()) {
            List<Case> urgencyCases = cases.stream()
                    .filter(c -> c.getUrgencyLevel().equals(urgency))
                    .collect(Collectors.toList());

            if (urgencyCases.isEmpty()) continue;

            double avgAssignment = calculateAvgAssignmentTimeByUrgency(urgencyCases);
            double avgResolution = urgencyCases.stream()
                    .filter(c -> c.getStatus().equals(CaseStatus.CLOSED) && c.getClosedAt() != null)
                    .mapToLong(c -> ChronoUnit.DAYS.between(c.getSubmittedAt(), c.getClosedAt()))
                    .average()
                    .orElse(0.0);

            int targetHours = getTargetHours(urgency);
            double slaCompliance = calculateUrgencySlaCompliance(urgencyCases, targetHours);

            performance.put(urgency.name(), CasePerformanceMetrics.UrgencyPerformance.builder()
                    .urgencyLevel(urgency.name())
                    .totalCases((long) urgencyCases.size())
                    .avgAssignmentTime(round(avgAssignment, 2))
                    .avgResolutionTime(round(avgResolution, 2))
                    .slaCompliance(round(slaCompliance, 2))
                    .build());
        }

        return performance;
    }

    /**
     * Get SLA target hours for urgency level
     */
    private int getTargetHours(UrgencyLevel urgency) {
        switch (urgency) {
            case CRITICAL:
                return 1;
            case HIGH:
                return 4;
            case MEDIUM:
                return 24;
            case LOW:
                return 48;
            default:
                return 24;
        }
    }

}