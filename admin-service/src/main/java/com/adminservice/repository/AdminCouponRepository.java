package com.adminservice.repository;

import com.adminservice.entity.AdminCoupon;
import com.commonlibrary.entity.AdminCouponStatus;
import com.commonlibrary.entity.BeneficiaryType;
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
 * Repository for AdminCoupon entity.
 * Provides comprehensive query methods for coupon management.
 */
@Repository
public interface AdminCouponRepository extends JpaRepository<AdminCoupon, Long> {

    // ==================== Basic Lookups ====================

    /**
     * Find coupon by code
     */
    Optional<AdminCoupon> findByCouponCodeAndIsDeletedFalse(String couponCode);

    /**
     * Find coupon by code (including deleted)
     */
    Optional<AdminCoupon> findByCouponCode(String couponCode);

    /**
     * Check if coupon code exists
     */
    boolean existsByCouponCode(String couponCode);

    // ==================== Status-based Queries ====================

    /**
     * Find all coupons by status
     */
    List<AdminCoupon> findByStatusAndIsDeletedFalse(AdminCouponStatus status);

    /**
     * Find coupons by status with pagination
     */
    Page<AdminCoupon> findByStatusAndIsDeletedFalse(AdminCouponStatus status, Pageable pageable);

    /**
     * Find coupons by multiple statuses
     */
    List<AdminCoupon> findByStatusInAndIsDeletedFalse(List<AdminCouponStatus> statuses);

    // ==================== Beneficiary-based Queries ====================

    /**
     * Find coupons by beneficiary type and ID
     */
    List<AdminCoupon> findByBeneficiaryTypeAndBeneficiaryIdAndIsDeletedFalse(
            BeneficiaryType beneficiaryType, Long beneficiaryId);

    /**
     * Find coupons by beneficiary type and ID with pagination
     */
    Page<AdminCoupon> findByBeneficiaryTypeAndBeneficiaryIdAndIsDeletedFalse(
            BeneficiaryType beneficiaryType, Long beneficiaryId, Pageable pageable);

    /**
     * Find available coupons for a beneficiary
     */
    @Query("SELECT c FROM AdminCoupon c WHERE c.beneficiaryType = :type " +
           "AND c.beneficiaryId = :beneficiaryId " +
           "AND c.status = 'DISTRIBUTED' " +
           "AND c.expiresAt > :now " +
           "AND c.isDeleted = false")
    List<AdminCoupon> findAvailableCouponsForBeneficiary(
            @Param("type") BeneficiaryType type,
            @Param("beneficiaryId") Long beneficiaryId,
            @Param("now") LocalDateTime now);

    /**
     * Find coupons by beneficiary type only
     */
    List<AdminCoupon> findByBeneficiaryTypeAndIsDeletedFalse(BeneficiaryType beneficiaryType);

    /**
     * Find unassigned coupons (in pool)
     */
    @Query("SELECT c FROM AdminCoupon c WHERE c.beneficiaryId IS NULL " +
           "AND c.status = 'CREATED' AND c.isDeleted = false")
    List<AdminCoupon> findUnassignedCoupons();

    // ==================== Batch-based Queries ====================

    /**
     * Find coupons by batch ID
     */
    List<AdminCoupon> findByBatchIdAndIsDeletedFalse(Long batchId);

    /**
     * Count coupons by batch and status
     */
    @Query("SELECT COUNT(c) FROM AdminCoupon c WHERE c.batchId = :batchId " +
           "AND c.status = :status AND c.isDeleted = false")
    long countByBatchIdAndStatus(@Param("batchId") Long batchId, 
                                  @Param("status") AdminCouponStatus status);

    // ==================== Expiration Queries ====================

    /**
     * Find expired coupons that need status update
     */
    @Query("SELECT c FROM AdminCoupon c WHERE c.expiresAt < :now " +
           "AND c.status IN ('CREATED', 'DISTRIBUTED') " +
           "AND c.isDeleted = false")
    List<AdminCoupon> findExpiredCoupons(@Param("now") LocalDateTime now);

    /**
     * Find coupons expiring within specified days
     */
    @Query("SELECT c FROM AdminCoupon c WHERE c.expiresAt BETWEEN :now AND :futureDate " +
           "AND c.status = 'DISTRIBUTED' " +
           "AND c.isDeleted = false")
    List<AdminCoupon> findCouponsExpiringSoon(@Param("now") LocalDateTime now,
                                               @Param("futureDate") LocalDateTime futureDate);

    /**
     * Find coupons expiring soon for a specific beneficiary
     */
    @Query("SELECT c FROM AdminCoupon c WHERE c.beneficiaryType = :type " +
           "AND c.beneficiaryId = :beneficiaryId " +
           "AND c.expiresAt BETWEEN :now AND :futureDate " +
           "AND c.status = 'DISTRIBUTED' " +
           "AND c.isDeleted = false")
    List<AdminCoupon> findCouponsExpiringSoonForBeneficiary(
            @Param("type") BeneficiaryType type,
            @Param("beneficiaryId") Long beneficiaryId,
            @Param("now") LocalDateTime now,
            @Param("futureDate") LocalDateTime futureDate);

    // ==================== Statistics Queries ====================

    /**
     * Count coupons by status
     */
    long countByStatusAndIsDeletedFalse(AdminCouponStatus status);

    /**
     * Count coupons by beneficiary
     */
    long countByBeneficiaryTypeAndBeneficiaryIdAndIsDeletedFalse(
            BeneficiaryType beneficiaryType, Long beneficiaryId);

    /**
     * Count available coupons for beneficiary
     */
    @Query("SELECT COUNT(c) FROM AdminCoupon c WHERE c.beneficiaryType = :type " +
           "AND c.beneficiaryId = :beneficiaryId " +
           "AND c.status = 'DISTRIBUTED' " +
           "AND c.expiresAt > :now " +
           "AND c.isDeleted = false")
    long countAvailableCouponsForBeneficiary(
            @Param("type") BeneficiaryType type,
            @Param("beneficiaryId") Long beneficiaryId,
            @Param("now") LocalDateTime now);

    /**
     * Sum of discount values for available coupons
     */
    @Query("SELECT COALESCE(SUM(c.discountValue), 0) FROM AdminCoupon c " +
           "WHERE c.beneficiaryType = :type " +
           "AND c.beneficiaryId = :beneficiaryId " +
           "AND c.status = 'DISTRIBUTED' " +
           "AND c.expiresAt > :now " +
           "AND c.isDeleted = false")
    BigDecimal sumAvailableValueForBeneficiary(
            @Param("type") BeneficiaryType type,
            @Param("beneficiaryId") Long beneficiaryId,
            @Param("now") LocalDateTime now);

    /**
     * Get coupon statistics by status
     */
    @Query("SELECT c.status, COUNT(c) FROM AdminCoupon c " +
           "WHERE c.isDeleted = false GROUP BY c.status")
    List<Object[]> getCouponStatsByStatus();

    /**
     * Get coupon statistics by beneficiary type
     */
    @Query("SELECT c.beneficiaryType, COUNT(c), " +
           "SUM(CASE WHEN c.status = 'DISTRIBUTED' AND c.expiresAt > CURRENT_TIMESTAMP THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN c.status = 'USED' THEN 1 ELSE 0 END) " +
           "FROM AdminCoupon c WHERE c.isDeleted = false GROUP BY c.beneficiaryType")
    List<Object[]> getCouponStatsByBeneficiaryType();

    // ==================== Search & Filter ====================

    /**
     * Search coupons with multiple filters
     */
    @Query("SELECT c FROM AdminCoupon c WHERE c.isDeleted = false " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND (:beneficiaryType IS NULL OR c.beneficiaryType = :beneficiaryType) " +
           "AND (:beneficiaryId IS NULL OR c.beneficiaryId = :beneficiaryId) " +
           "AND (:batchId IS NULL OR c.batchId = :batchId) " +
           "AND (:couponCode IS NULL OR c.couponCode LIKE %:couponCode%)")
    Page<AdminCoupon> searchCoupons(
            @Param("status") AdminCouponStatus status,
            @Param("beneficiaryType") BeneficiaryType beneficiaryType,
            @Param("beneficiaryId") Long beneficiaryId,
            @Param("batchId") Long batchId,
            @Param("couponCode") String couponCode,
            Pageable pageable);

    // ==================== Bulk Update Operations ====================

    /**
     * Bulk update status for expired coupons
     */
    @Modifying
    @Query("UPDATE AdminCoupon c SET c.status = 'EXPIRED', c.updatedAt = :now " +
           "WHERE c.expiresAt < :now " +
           "AND c.status IN ('CREATED', 'DISTRIBUTED') " +
           "AND c.isDeleted = false")
    int updateExpiredCoupons(@Param("now") LocalDateTime now);

    /**
     * Bulk update status for batch distribution
     */
    @Modifying
    @Query("UPDATE AdminCoupon c SET c.status = 'DISTRIBUTED', " +
           "c.beneficiaryId = :beneficiaryId, " +
           "c.distributedBy = :distributedBy, " +
           "c.distributedAt = :distributedAt, " +
           "c.updatedAt = :distributedAt " +
           "WHERE c.batchId = :batchId " +
           "AND c.status = 'CREATED' " +
           "AND c.isDeleted = false")
    int distributeBatch(@Param("batchId") Long batchId,
                        @Param("beneficiaryId") Long beneficiaryId,
                        @Param("distributedBy") Long distributedBy,
                        @Param("distributedAt") LocalDateTime distributedAt);

    /**
     * Bulk cancel coupons by batch
     */
    @Modifying
    @Query("UPDATE AdminCoupon c SET c.status = 'CANCELLED', " +
           "c.cancellationReason = :reason, " +
           "c.cancelledBy = :cancelledBy, " +
           "c.cancelledAt = :cancelledAt, " +
           "c.updatedAt = :cancelledAt " +
           "WHERE c.batchId = :batchId " +
           "AND c.status IN ('CREATED', 'DISTRIBUTED') " +
           "AND c.isDeleted = false")
    int cancelBatch(@Param("batchId") Long batchId,
                    @Param("reason") String reason,
                    @Param("cancelledBy") Long cancelledBy,
                    @Param("cancelledAt") LocalDateTime cancelledAt);

    // ==================== Validation Queries ====================

    /**
     * Find coupon for validation (must be distributed and not expired)
     */
    @Query("SELECT c FROM AdminCoupon c WHERE c.couponCode = :couponCode " +
           "AND c.status = 'DISTRIBUTED' " +
           "AND c.expiresAt > :now " +
           "AND c.isDeleted = false")
    Optional<AdminCoupon> findValidCoupon(@Param("couponCode") String couponCode,
                                          @Param("now") LocalDateTime now);

    /**
     * Find coupon for validation with beneficiary check
     */
    @Query("SELECT c FROM AdminCoupon c WHERE c.couponCode = :couponCode " +
           "AND c.beneficiaryType = :beneficiaryType " +
           "AND c.beneficiaryId = :beneficiaryId " +
           "AND c.status = 'DISTRIBUTED' " +
           "AND c.expiresAt > :now " +
           "AND c.isDeleted = false")
    Optional<AdminCoupon> findValidCouponForBeneficiary(
            @Param("couponCode") String couponCode,
            @Param("beneficiaryType") BeneficiaryType beneficiaryType,
            @Param("beneficiaryId") Long beneficiaryId,
            @Param("now") LocalDateTime now);
}