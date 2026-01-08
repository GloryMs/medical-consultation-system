package com.supervisorservice.service;

import com.commonlibrary.dto.AppointmentDto;
import com.commonlibrary.dto.CaseDto;
import com.commonlibrary.dto.PaymentHistoryDto;
import com.commonlibrary.entity.CaseStatus;
import com.supervisorservice.feign.DoctorServiceClient;
import com.supervisorservice.feign.PatientServiceClient;
import com.supervisorservice.feign.PaymentServiceClient;
import com.supervisorservice.dto.CouponSummaryDto;
import com.supervisorservice.dto.RecentActivityDto;
import com.supervisorservice.dto.SupervisorDashboardDto;
import com.supervisorservice.entity.MedicalSupervisor;
import com.supervisorservice.repository.SupervisorPatientAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for supervisor dashboard statistics and recent activity
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupervisorDashboardService {

    private final SupervisorValidationService validationService;
    private final SupervisorPatientAssignmentRepository assignmentRepository;
    private final CouponService couponService;
    private final PatientServiceClient patientServiceClient;
    private final DoctorServiceClient doctorServiceClient;
    private final PaymentServiceClient paymentServiceClient;

    /**
     * Get comprehensive dashboard statistics
     */
    @Transactional(readOnly = true)
    public SupervisorDashboardDto getDashboardStatistics(Long userId) {
        log.debug("Getting dashboard statistics for supervisor userId: {}", userId);

        MedicalSupervisor supervisor = validationService.validateSupervisorActive(userId);

        // Get patient count
        long activePatientCount = assignmentRepository.countActiveAssignmentsBySupervisor(supervisor.getId());
        log.info("Active Patients count: {}", activePatientCount);

        // Get patient IDs
        List<Long> patientIds = assignmentRepository.findPatientIdsBySupervisor(supervisor.getId());
        log.info("List of patient IDs: {}", patientIds!=null ? patientIds : "Empty list");

        // Get case statistics from patient-service
        Long totalCases = 0L;
        Long activeCases = 0L;
        Long completedCases = 0L;

        log.info("Trying to get supervisor dashboard statistics:");

        try {
            assert patientIds != null;
            for (Long patientId : patientIds) {
                List<CaseDto> cases = patientServiceClient.getAllCasesForAdmin(
                        null, null, null, patientId, null, null, null, null).getData();

                if (cases != null) {
                    totalCases += cases.size();

                    long active = cases.stream()
                            .filter(c -> c.getStatus() == CaseStatus.PENDING ||
                                    c.getStatus() == CaseStatus.ASSIGNED ||
                                    c.getStatus() == CaseStatus.IN_PROGRESS)
                            .count();
                    activeCases += active;

                    log.info("Active cases count: {}", activeCases);

                    long completed = cases.stream()
                            .filter(c -> c.getStatus() == CaseStatus.CLOSED)
                            .count();
                    completedCases += completed;
                    log.info("Completed cases count: {}", completedCases);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error fetching case statistics from patient-service: {}", e.getMessage());
        }

        // Get coupon statistics
        CouponSummaryDto couponSummary = couponService.getCouponSummary(userId);
        log.info("Getting couponSummary DTO");
        // Get appointment statistics from doctor-service
        Integer totalAppointments = 0;
        Integer upcomingAppointments = 0;
        Integer completedAppointments = 0;

        try {
            List<AppointmentDto> allAppointments = new ArrayList<>();

            for (Long patientId : patientIds) {
                try {
                    List<AppointmentDto> appointments = doctorServiceClient
                            .getPatientAppointments(patientId).getBody().getData();
                    if (appointments != null) {
                        allAppointments.addAll(appointments);
                    }
                } catch (Exception e) {
                    log.error("Error fetching appointments for patient {}: {}", patientId, e.getMessage());
                }
            }

            if (!allAppointments.isEmpty()) {
                totalAppointments = allAppointments.size();

                upcomingAppointments = (int) allAppointments.stream()
                        .filter(a -> a.getStatus() != null &&
                                "SCHEDULED".equals(a.getStatus().name()) &&
                                a.getScheduledTime() != null &&
                                a.getScheduledTime().isAfter(LocalDateTime.now()))
                        .count();

                log.info("UpcomingAppointments cases count: {}", upcomingAppointments);

                completedAppointments = (int) allAppointments.stream()
                        .filter(a -> a.getStatus() != null &&
                                "COMPLETED".equals(a.getStatus().name()))
                        .count();
                log.info("CompletedAppointments cases count: {}", completedAppointments);
            }
        } catch (Exception e) {
            log.error("Error fetching appointment statistics from doctor-service: {}", e.getMessage());
        }

        // Get payment statistics from payment-service
        Integer totalPayments = 0;
        java.math.BigDecimal totalPaymentAmount = java.math.BigDecimal.ZERO;

        try {
            for (Long patientId : patientIds) {
                List<PaymentHistoryDto> payments = paymentServiceClient
                        .getPatientPaymentHistory(patientId).getData();

                if (payments != null) {
                    totalPayments += payments.size();
                    log.info("TotalPayments cases count: {}", totalPayments);
                    totalPaymentAmount = totalPaymentAmount.add(
                            payments.stream()
                                    .map(PaymentHistoryDto::getAmount)
                                    .filter(amount -> amount != null)
                                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                    );
                    log.info("TotalPaymentAmount cases count: {}", totalPaymentAmount);
                }
            }
        } catch (Exception e) {
            log.error("Error fetching payment statistics from payment-service: {}", e.getMessage());
        }

        // Build dashboard DTO
        log.info("Now building supervisor dashboard DTO");
        return SupervisorDashboardDto.builder()
                .supervisorId(supervisor.getId())
                .supervisorName(supervisor.getFullName())
                .verificationStatus(supervisor.getVerificationStatus())
                .activePatientCount((int) activePatientCount)
                .maxPatientsLimit(supervisor.getMaxPatientsLimit())
                .totalCasesSubmitted(totalCases.intValue())
                .activeCases(activeCases.intValue())
                .completedCases(completedCases.intValue())
                .totalAppointments(totalAppointments)
                .upcomingAppointments(upcomingAppointments)
                .completedAppointments(completedAppointments)
                .totalCouponsIssued(couponSummary.getTotalCoupons())
                .availableCoupons(couponSummary.getAvailableCoupons())
                .usedCoupons(couponSummary.getUsedCoupons())
                .totalCouponValue(couponSummary.getTotalAvailableValue())
                .totalPaymentsProcessed(totalPayments)
                .lastActivityAt(LocalDateTime.now())
                .build();
    }

    /**
     * Get recent activity timeline
     */
    @Transactional(readOnly = true)
    public List<RecentActivityDto> getRecentActivity(Long userId, Integer limit) {
        log.debug("Getting recent activity for supervisor userId: {}, limit: {}", userId, limit);

        MedicalSupervisor supervisor = validationService.validateSupervisorActive(userId);

        List<RecentActivityDto> activities = new ArrayList<>();

        // Get patient IDs
        List<Long> patientIds = assignmentRepository.findPatientIdsBySupervisor(supervisor.getId());

        // Fetch recent cases as activities
        try {
            for (Long patientId : patientIds) {
                List<CaseDto> cases = patientServiceClient.getAllCasesForAdmin(
                        null, null, null, patientId, null, null, null, null).getData();

                if (cases != null) {
                    // Add case submissions
                    activities.addAll(cases.stream()
                            .sorted((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt()))
                            .limit(limit != null ? limit : 10)
                            .map(caseDto -> {
                                String caseDescription = caseDto.getCaseTitle() != null
                                        ? caseDto.getCaseTitle()
                                        : (caseDto.getDescription() != null ? caseDto.getDescription() : "No description");

                                return RecentActivityDto.builder()
                                        .activityType("CASE_SUBMITTED")
                                        .title("Case Submitted")
                                        .description(String.format("Case submitted for patient: %s - %s",
                                                caseDto.getPatientName() != null ? caseDto.getPatientName() : "Unknown",
                                                caseDescription))
                                        .relatedEntityId(caseDto.getId())
                                        .relatedEntityType("CASE")
                                        .timestamp(caseDto.getCreatedAt())
                                        .build();
                            })
                            .collect(Collectors.toList()));
                }
            }
        } catch (Exception e) {
            log.error("Error fetching recent activities: {}", e.getMessage());
        }

        // Sort by timestamp and limit
        return activities.stream()
                .sorted((a1, a2) -> a2.getTimestamp().compareTo(a1.getTimestamp()))
                .limit(limit != null ? limit : 20)
                .collect(Collectors.toList());
    }

    /**
     * Get performance metrics
     */
    @Transactional(readOnly = true)
    public Object getPerformanceMetrics(Long userId) {
        log.debug("Getting performance metrics for supervisor userId: {}", userId);

        // Placeholder for performance metrics
        // Would aggregate data from multiple services

        return java.util.Map.of(
                "message", "Performance metrics endpoint - to be fully implemented with all service integrations",
                "note", "Requires data from patient-service, doctor-service, and payment-service"
        );
    }
}