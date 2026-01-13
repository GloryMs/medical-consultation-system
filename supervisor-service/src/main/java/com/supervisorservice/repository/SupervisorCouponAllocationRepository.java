package com.supervisorservice.repository;

import com.commonlibrary.entity.SupervisorCouponStatus;
import com.supervisorservice.entity.SupervisorCouponAllocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for SupervisorCouponAllocation entity.
 * Manages local coupon allocation data in supervisor-service.
 */
@Repository
public interface SupervisorCouponAllocationRepository extends JpaRepository<SupervisorCouponAllocation, Long> {

    // ==================== Basic Lookups ====================

    /**
     * Find allocation by coupon code and supervisor
     */
    Optional<SupervisorCouponAllocation> findByCouponCodeAndSupervisorId(String couponCode, Long supervisorId);

    /**
     * Find allocation by admin coupon ID
     */
    Optional<SupervisorCouponAllocation> findByAdminCouponId(Long adminCouponId);

    /**
     * Check if coupon code exists for supervisor
     */
    boolean existsByCouponCodeAndSupervisorId(String couponCode, Long supervisorId);

    /**
     * Check if admin coupon ID exists
     */
    boolean existsByAdminCouponId(Long adminCouponId);

    // ==================== Supervisor-based Queries ====================

    /**
     * Find all allocations for a supervisor
     */
    List<SupervisorCouponAllocation> findBySupervisorIdOrderByCreatedAtDesc(Long supervisorId);

    /**
     * Find allocations for supervisor with pagination
     */
    Page<SupervisorCouponAllocation> findBySupervisorId(Long supervisorId, Pageable pageable);

    /**
     * Find allocations by supervisor and status
     */
    List<SupervisorCouponAllocation> findBySupervisorIdAndStatus(Long supervisorId, SupervisorCouponStatus status);

    /**
     * Find available (unassigned) coupons for supervisor
     */
    @Query("SELECT a FROM SupervisorCouponAllocation a WHERE a.supervisorId = :supervisorId " +
           "AND a.status = 'AVAILABLE' " +
           "AND a.assignedPatientId IS NULL " +
           "AND a.expiresAt > :now " +
           "ORDER BY a.expiresAt ASC")
    List<SupervisorCouponAllocation> findUnassignedCouponsForSupervisor(
            @Param("supervisorId") Long supervisorId,
            @Param("now") LocalDateTime now);

    /**
     * Find assigned (ready to use) coupons for supervisor
     */
    @Query("SELECT a FROM SupervisorCouponAllocation a WHERE a.supervisorId = :supervisorId " +
           "AND a.status = 'ASSIGNED' " +
           "AND a.assignedPatientId IS NOT NULL " +
           "AND a.expiresAt > :now " +
           "ORDER BY a.expiresAt ASC")
    List<SupervisorCouponAllocation> findAssignedCouponsForSupervisor(
            @Param("supervisorId") Long supervisorId,
            @Param("now") LocalDateTime now);

    // ==================== Patient-based Queries ====================

    /**
     * Find coupons assigned to a specific patient
     */
    List<SupervisorCouponAllocation> findBySupervisorIdAndAssignedPatientIdOrderByCreatedAtDesc(
            Long supervisorId, Long patientId);

    /**
     * Find available coupons for a specific patient
     */
    @Query("SELECT a FROM SupervisorCouponAllocation a WHERE a.supervisorId = :supervisorId " +
           "AND a.assignedPatientId = :patientId " +
           "AND a.status = 'ASSIGNED' " +
           "AND a.expiresAt > :now " +
           "ORDER BY a.expiresAt ASC")
    List<SupervisorCouponAllocation> findAvailableCouponsForPatient(
            @Param("supervisorId") Long supervisorId,
            @Param("patientId") Long patientId,
            @Param("now") LocalDateTime now);

    /**
     * Find coupon for patient by code
     */
    @Query("SELECT a FROM SupervisorCouponAllocation a WHERE a.supervisorId = :supervisorId " +
           "AND a.assignedPatientId = :patientId " +
           "AND a.couponCode = :couponCode " +
           "AND a.status = 'ASSIGNED' " +
           "AND a.expiresAt > :now")
    Optional<SupervisorCouponAllocation> findValidCouponForPatient(
            @Param("supervisorId") Long supervisorId,
            @Param("patientId") Long patientId,
            @Param("couponCode") String couponCode,
            @Param("now") LocalDateTime now);

    // ==================== Expiration Queries ====================

    /**
     * Find expiring coupons for supervisor
     */
    @Query("SELECT a FROM SupervisorCouponAllocation a WHERE a.supervisorId = :supervisorId " +
           "AND a.expiresAt BETWEEN :now AND :futureDate " +
            "AND a.status IN (com.commonlibrary.entity.SupervisorCouponStatus.AVAILABLE, " +
            "                 com.commonlibrary.entity.SupervisorCouponStatus.ASSIGNED) " +
           "ORDER BY a.expiresAt ASC")
    List<SupervisorCouponAllocation> findExpiringSoonForSupervisor(
            @Param("supervisorId") Long supervisorId,
            @Param("now") LocalDateTime now,
            @Param("futureDate") LocalDateTime futureDate);
    /**
     * Find expired coupons that need status update
     */
    @Query("SELECT a FROM SupervisorCouponAllocation a WHERE a.expiresAt < :now " +
            "AND a.status IN (com.commonlibrary.entity.SupervisorCouponStatus.AVAILABLE, " +
            "                 com.commonlibrary.entity.SupervisorCouponStatus.ASSIGNED) ")
    List<SupervisorCouponAllocation> findExpiredCoupons(@Param("now") LocalDateTime now);

    // ==================== Statistics Queries ====================

    /**
     * Count coupons by supervisor and status
     */
    long countBySupervisorIdAndStatus(Long supervisorId, SupervisorCouponStatus status);

    /**
     * Count available coupons for supervisor
     */
    @Query("SELECT COUNT(a) FROM SupervisorCouponAllocation a WHERE a.supervisorId = :supervisorId " +
            "AND a.status IN (com.commonlibrary.entity.SupervisorCouponStatus.AVAILABLE, " +
            "                 com.commonlibrary.entity.SupervisorCouponStatus.ASSIGNED) " +
           "AND a.expiresAt > :now")
    long countAvailableCouponsForSupervisor(
            @Param("supervisorId") Long supervisorId,
            @Param("now") LocalDateTime now);

    /**
     * Sum available value for supervisor
     */
    @Query("SELECT COALESCE(SUM(a.discountValue), 0) FROM SupervisorCouponAllocation a " +
           "WHERE a.supervisorId = :supervisorId " +
            "AND a.status IN (com.commonlibrary.entity.SupervisorCouponStatus.AVAILABLE, " +
            "                 com.commonlibrary.entity.SupervisorCouponStatus.ASSIGNED) " +
           "AND a.expiresAt > :now")
    BigDecimal sumAvailableValueForSupervisor(
            @Param("supervisorId") Long supervisorId,
            @Param("now") LocalDateTime now);

    /**
     * Count available coupons for patient
     */
    @Query("SELECT COUNT(a) FROM SupervisorCouponAllocation a WHERE a.supervisorId = :supervisorId " +
           "AND a.assignedPatientId = :patientId " +
           "AND a.status = 'ASSIGNED' " +
           "AND a.expiresAt > :now")
    long countAvailableCouponsForPatient(
            @Param("supervisorId") Long supervisorId,
            @Param("patientId") Long patientId,
            @Param("now") LocalDateTime now);

    // ==================== Bulk Update Operations ====================

    /**
     * Update status for expired coupons
     */
    @Modifying
    @Query("UPDATE SupervisorCouponAllocation a SET a.status = 'EXPIRED', a.updatedAt = :now " +
           "WHERE a.expiresAt < :now " +
            "AND a.status IN (com.commonlibrary.entity.SupervisorCouponStatus.AVAILABLE, " +
            "                 com.commonlibrary.entity.SupervisorCouponStatus.ASSIGNED) ")
    int updateExpiredCoupons(@Param("now") LocalDateTime now);

    /**
     * Update coupon status by admin coupon ID (for Kafka sync)
     */
    @Modifying
    @Query("UPDATE SupervisorCouponAllocation a SET a.status = :status, " +
           "a.lastSyncedAt = :syncTime, a.updatedAt = :syncTime " +
           "WHERE a.adminCouponId = :adminCouponId")
    int updateStatusByAdminCouponId(
            @Param("adminCouponId") Long adminCouponId,
            @Param("status") SupervisorCouponStatus status,
            @Param("syncTime") LocalDateTime syncTime);

    /**
     * Mark coupon as used
     */
    @Modifying
    @Query("UPDATE SupervisorCouponAllocation a SET a.status = 'USED', " +
           "a.usedAt = :usedAt, a.usedForCaseId = :caseId, a.usedForPaymentId = :paymentId, " +
           "a.lastSyncedAt = :usedAt, a.updatedAt = :usedAt " +
           "WHERE a.adminCouponId = :adminCouponId")
    int markAsUsed(
            @Param("adminCouponId") Long adminCouponId,
            @Param("caseId") Long caseId,
            @Param("paymentId") Long paymentId,
            @Param("usedAt") LocalDateTime usedAt);

    /**
     * Mark coupon as cancelled
     */
    @Modifying
    @Query("UPDATE SupervisorCouponAllocation a SET a.status = 'CANCELLED', " +
           "a.lastSyncedAt = :cancelledAt, a.updatedAt = :cancelledAt " +
           "WHERE a.adminCouponId = :adminCouponId")
    int markAsCancelled(
            @Param("adminCouponId") Long adminCouponId,
            @Param("cancelledAt") LocalDateTime cancelledAt);

    // ==================== Search ====================

    /**
     * Search allocations with filters
     */
    @Query("SELECT a FROM SupervisorCouponAllocation a WHERE a.supervisorId = :supervisorId " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (:patientId IS NULL OR a.assignedPatientId = :patientId) " +
           "AND (:couponCode IS NULL OR a.couponCode LIKE %:couponCode%)")
    Page<SupervisorCouponAllocation> searchAllocations(
            @Param("supervisorId") Long supervisorId,
            @Param("status") SupervisorCouponStatus status,
            @Param("patientId") Long patientId,
            @Param("couponCode") String couponCode,
            Pageable pageable);
}