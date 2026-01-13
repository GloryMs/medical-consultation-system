package com.adminservice.repository;

import com.adminservice.entity.CouponRedemptionHistory;
import com.commonlibrary.entity.BeneficiaryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for CouponRedemptionHistory entity.
 * Provides query methods for coupon redemption audit trail.
 */
@Repository
public interface CouponRedemptionHistoryRepository extends JpaRepository<CouponRedemptionHistory, Long> {

    // ==================== Basic Lookups ====================

    /**
     * Find redemption by coupon ID
     */
    Optional<CouponRedemptionHistory> findByCouponId(Long couponId);

    /**
     * Find redemption by coupon code
     */
    Optional<CouponRedemptionHistory> findByCouponCode(String couponCode);

    /**
     * Find redemption by payment ID
     */
    Optional<CouponRedemptionHistory> findByPaymentId(Long paymentId);

    // ==================== Case-based Queries ====================

    /**
     * Find redemptions by case ID
     */
    List<CouponRedemptionHistory> findByCaseId(Long caseId);

    /**
     * Check if case has coupon redemption
     */
    boolean existsByCaseId(Long caseId);

    // ==================== Patient-based Queries ====================

    /**
     * Find redemptions by patient ID
     */
    List<CouponRedemptionHistory> findByPatientIdOrderByRedeemedAtDesc(Long patientId);

    /**
     * Find redemptions by patient ID with pagination
     */
    Page<CouponRedemptionHistory> findByPatientId(Long patientId, Pageable pageable);

    // ==================== Redeemer-based Queries ====================

    /**
     * Find redemptions by redeemer type and ID
     */
    List<CouponRedemptionHistory> findByRedeemedByTypeAndRedeemedByIdOrderByRedeemedAtDesc(
            BeneficiaryType redeemedByType, Long redeemedById);

    /**
     * Find redemptions by redeemer type and ID with pagination
     */
    Page<CouponRedemptionHistory> findByRedeemedByTypeAndRedeemedById(
            BeneficiaryType redeemedByType, Long redeemedById, Pageable pageable);

    // ==================== Date Range Queries ====================

    /**
     * Find redemptions within date range
     */
    List<CouponRedemptionHistory> findByRedeemedAtBetweenOrderByRedeemedAtDesc(
            LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find redemptions within date range with pagination
     */
    Page<CouponRedemptionHistory> findByRedeemedAtBetween(
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    // ==================== Statistics Queries ====================

    /**
     * Count total redemptions
     */
    long count();

    /**
     * Count redemptions by redeemer type
     */
    long countByRedeemedByType(BeneficiaryType redeemedByType);

    /**
     * Sum of discounts applied
     */
    @Query("SELECT COALESCE(SUM(r.discountApplied), 0) FROM CouponRedemptionHistory r")
    BigDecimal sumTotalDiscountsApplied();

    /**
     * Sum of discounts applied by redeemer type
     */
    @Query("SELECT COALESCE(SUM(r.discountApplied), 0) FROM CouponRedemptionHistory r " +
           "WHERE r.redeemedByType = :type")
    BigDecimal sumDiscountsAppliedByRedeemerType(@Param("type") BeneficiaryType type);

    /**
     * Sum of discounts applied within date range
     */
    @Query("SELECT COALESCE(SUM(r.discountApplied), 0) FROM CouponRedemptionHistory r " +
           "WHERE r.redeemedAt BETWEEN :startDate AND :endDate")
    BigDecimal sumDiscountsAppliedInDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get redemption statistics by redeemer type
     */
    @Query("SELECT r.redeemedByType, COUNT(r), SUM(r.discountApplied), SUM(r.finalAmount) " +
           "FROM CouponRedemptionHistory r GROUP BY r.redeemedByType")
    List<Object[]> getRedemptionStatsByRedeemerType();

    /**
     * Get daily redemption statistics
     */
    @Query("SELECT CAST(r.redeemedAt AS date), COUNT(r), SUM(r.discountApplied) " +
           "FROM CouponRedemptionHistory r " +
           "WHERE r.redeemedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY CAST(r.redeemedAt AS date) " +
           "ORDER BY CAST(r.redeemedAt AS date)")
    List<Object[]> getDailyRedemptionStats(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get monthly redemption statistics
     */
    @Query("SELECT YEAR(r.redeemedAt), MONTH(r.redeemedAt), COUNT(r), SUM(r.discountApplied) " +
           "FROM CouponRedemptionHistory r " +
           "WHERE r.redeemedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY YEAR(r.redeemedAt), MONTH(r.redeemedAt) " +
           "ORDER BY YEAR(r.redeemedAt), MONTH(r.redeemedAt)")
    List<Object[]> getMonthlyRedemptionStats(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ==================== Search & Filter ====================

    /**
     * Search redemptions with multiple filters
     */
    @Query("SELECT r FROM CouponRedemptionHistory r " +
           "WHERE (:redeemedByType IS NULL OR r.redeemedByType = :redeemedByType) " +
           "AND (:redeemedById IS NULL OR r.redeemedById = :redeemedById) " +
           "AND (:patientId IS NULL OR r.patientId = :patientId) " +
           "AND (:couponCode IS NULL OR r.couponCode LIKE %:couponCode%) " +
           "AND (:startDate IS NULL OR r.redeemedAt >= :startDate) " +
           "AND (:endDate IS NULL OR r.redeemedAt <= :endDate)")
    Page<CouponRedemptionHistory> searchRedemptions(
            @Param("redeemedByType") BeneficiaryType redeemedByType,
            @Param("redeemedById") Long redeemedById,
            @Param("patientId") Long patientId,
            @Param("couponCode") String couponCode,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // ==================== Recent Redemptions ====================

    /**
     * Find recent redemptions
     */
    @Query("SELECT r FROM CouponRedemptionHistory r ORDER BY r.redeemedAt DESC")
    List<CouponRedemptionHistory> findRecentRedemptions(Pageable pageable);

    /**
     * Find recent redemptions for redeemer
     */
    @Query("SELECT r FROM CouponRedemptionHistory r " +
           "WHERE r.redeemedByType = :type AND r.redeemedById = :redeemedById " +
           "ORDER BY r.redeemedAt DESC")
    List<CouponRedemptionHistory> findRecentRedemptionsForRedeemer(
            @Param("type") BeneficiaryType type,
            @Param("redeemedById") Long redeemedById,
            Pageable pageable);
}