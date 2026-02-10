package com.commonlibrary.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Comprehensive DTO for platform-wide supervisor statistics
 * Used by admin dashboard to display supervisor system metrics
 */
@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupervisorStatisticsDto {

    // ===== Overall Supervisor Metrics =====

    /**
     * Total number of supervisors in the system (excluding deleted)
     */
    private Long totalSupervisors;

    /**
     * Number of active (verified and available) supervisors
     */
    private Long activeSupervisors;

    /**
     * Number of supervisors pending verification
     */
    private Long pendingSupervisors;

    /**
     * Number of verified supervisors
     */
    private Long verifiedSupervisors;

    /**
     * Number of rejected supervisors
     */
    private Long rejectedSupervisors;

    /**
     * Number of suspended supervisors
     */
    private Long suspendedSupervisors;

    /**
     * Breakdown of supervisors by verification status
     * Key: Status name (PENDING, VERIFIED, REJECTED, SUSPENDED)
     * Value: Count
     */
    private Map<String, Long> supervisorsByStatus;


    // ===== Patient Assignment Metrics =====

    /**
     * Total number of patient-supervisor assignments (all statuses)
     */
    private Long totalPatientAssignments;

    /**
     * Number of active patient-supervisor assignments
     */
    private Long activePatientAssignments;

    /**
     * Number of inactive patient-supervisor assignments
     */
    private Long inactivePatientAssignments;

    /**
     * Average number of patients per active supervisor
     */
    private Double averagePatientsPerSupervisor;

    /**
     * Total number of unique patients being managed by supervisors
     */
    private Long totalUniquePatientsManaged;


    // ===== Coupon Metrics =====

    /**
     * Total number of coupons issued
     */
    private Long totalCouponsIssued;

    /**
     * Number of available (unused, non-expired) coupons
     */
    private Long availableCoupons;

    /**
     * Number of used coupons
     */
    private Long usedCoupons;

    /**
     * Number of expired coupons
     */
    private Long expiredCoupons;

    /**
     * Number of cancelled coupons
     */
    private Long cancelledCoupons;

    /**
     * Number of coupons expiring soon (within 7 days)
     */
    private Long couponsExpiringSoon;

    /**
     * Total value of available coupons
     */
    private BigDecimal totalAvailableCouponValue;

    /**
     * Breakdown of coupons by status
     * Key: Status name (AVAILABLE, USED, EXPIRED, CANCELLED)
     * Value: Count
     */
    private Map<String, Long> couponsByStatus;


    // ===== Payment Metrics =====

    /**
     * Total number of payments processed by supervisors
     */
    private Long totalPaymentsProcessed;

    /**
     * Number of completed payments
     */
    private Long completedPayments;

    /**
     * Number of pending payments
     */
    private Long pendingPayments;

    /**
     * Number of failed payments
     */
    private Long failedPayments;

    /**
     * Total amount paid by supervisors on behalf of patients
     */
    private BigDecimal totalAmountPaid;

    /**
     * Total discount amount from coupons
     */
    private BigDecimal totalDiscountAmount;

    /**
     * Breakdown of payments by payment method
     * Key: Payment method (STRIPE, PAYPAL, COUPON)
     * Value: Count
     */
    private Map<String, Long> paymentsByMethod;

    /**
     * Average payment amount per consultation
     */
    private BigDecimal averagePaymentAmount;


    // ===== Capacity Metrics =====

    /**
     * Number of supervisors with available capacity (can add more patients)
     */
    private Long supervisorsWithCapacity;

    /**
     * Average capacity utilization percentage across all supervisors
     */
    private Double averageCapacityUtilization;

    /**
     * Total patient capacity (sum of all supervisor limits)
     */
    private Long totalPatientCapacity;

    /**
     * Used patient capacity (total active assignments)
     */
    private Long usedPatientCapacity;


    // ===== Recent Activity Metrics =====

    /**
     * Number of supervisors registered in the last 30 days
     */
    private Long recentRegistrations;

    /**
     * Number of supervisors verified in the last 30 days
     */
    private Long recentVerifications;

    /**
     * Number of patient assignments made in the last 30 days
     */
    private Long recentAssignments;

    /**
     * Number of payments processed in the last 30 days
     */
    private Long recentPayments;
}