package com.supervisorservice.repository;

import com.commonlibrary.entity.CouponStatus;
import com.supervisorservice.entity.SupervisorCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for SupervisorCoupon entity
 */
@Repository
public interface SupervisorCouponRepository extends JpaRepository<SupervisorCoupon, Long> {
    
    /**
     * Find coupon by code
     */
    Optional<SupervisorCoupon> findByCouponCode(String couponCode);
    
    /**
     * Find coupon by code and not deleted
     */
    Optional<SupervisorCoupon> findByCouponCodeAndIsDeletedFalse(String couponCode);
    
    /**
     * Find available coupon by code
     */
    @Query("SELECT c FROM SupervisorCoupon c WHERE c.couponCode = :code " +
           "AND c.status = 'AVAILABLE' AND c.isDeleted = false " +
           "AND c.expiresAt > :now")
    Optional<SupervisorCoupon> findAvailableCoupon(
            @Param("code") String code,
            @Param("now") LocalDateTime now);
    
    /**
     * Find all coupons for a supervisor
     */
    @Query("SELECT c FROM SupervisorCoupon c WHERE c.supervisor.id = :supervisorId " +
           "AND c.isDeleted = false ORDER BY c.issuedAt DESC")
    List<SupervisorCoupon> findBySupervisorId(@Param("supervisorId") Long supervisorId);
    
    /**
     * Find all coupons for a patient
     */
    @Query("SELECT c FROM SupervisorCoupon c WHERE c.patientId = :patientId " +
           "AND c.isDeleted = false ORDER BY c.issuedAt DESC")
    List<SupervisorCoupon> findByPatientId(@Param("patientId") Long patientId);
    
    /**
     * Find available coupons for a patient
     */
    @Query("SELECT c FROM SupervisorCoupon c WHERE c.patientId = :patientId " +
           "AND c.status = 'AVAILABLE' AND c.isDeleted = false " +
           "AND c.expiresAt > :now ORDER BY c.expiresAt ASC")
    List<SupervisorCoupon> findAvailableByPatientId(
            @Param("patientId") Long patientId,
            @Param("now") LocalDateTime now);
    
    /**
     * Find available coupons for a supervisor and patient
     */
    @Query("SELECT c FROM SupervisorCoupon c WHERE c.supervisor.id = :supervisorId " +
           "AND c.patientId = :patientId AND c.status = 'AVAILABLE' " +
           "AND c.isDeleted = false AND c.expiresAt > :now ORDER BY c.expiresAt ASC")
    List<SupervisorCoupon> findAvailableBySupervisorAndPatient(
            @Param("supervisorId") Long supervisorId,
            @Param("patientId") Long patientId,
            @Param("now") LocalDateTime now);
    
    /**
     * Find coupons by status
     */
    List<SupervisorCoupon> findByStatusAndIsDeletedFalse(CouponStatus status);
    
    /**
     * Find coupons by batch ID
     */
    @Query("SELECT c FROM SupervisorCoupon c WHERE c.batch.id = :batchId " +
           "AND c.isDeleted = false ORDER BY c.issuedAt DESC")
    List<SupervisorCoupon> findByBatchId(@Param("batchId") Long batchId);
    
    /**
     * Find expired coupons that are still marked as AVAILABLE
     */
    @Query("SELECT c FROM SupervisorCoupon c WHERE c.status = 'AVAILABLE' " +
           "AND c.isDeleted = false AND c.expiresAt < :now")
    List<SupervisorCoupon> findExpiredAvailableCoupons(@Param("now") LocalDateTime now);
    
    /**
     * Find coupons expiring soon (within warning days)
     */
    @Query("SELECT c FROM SupervisorCoupon c WHERE c.status = 'AVAILABLE' " +
           "AND c.isDeleted = false AND c.expiresAt BETWEEN :now AND :warningDate")
    List<SupervisorCoupon> findCouponsExpiringSoon(
            @Param("now") LocalDateTime now,
            @Param("warningDate") LocalDateTime warningDate);
    
    /**
     * Count available coupons for a patient
     */
    @Query("SELECT COUNT(c) FROM SupervisorCoupon c WHERE c.patientId = :patientId " +
           "AND c.status = 'AVAILABLE' AND c.isDeleted = false AND c.expiresAt > :now")
    Long countAvailableByPatientId(@Param("patientId") Long patientId, @Param("now") LocalDateTime now);
    
    /**
     * Count coupons by status for a supervisor
     */
    @Query("SELECT c.status, COUNT(c) FROM SupervisorCoupon c " +
           "WHERE c.supervisor.id = :supervisorId AND c.isDeleted = false " +
           "GROUP BY c.status")
    List<Object[]> countByStatusForSupervisor(@Param("supervisorId") Long supervisorId);
    
    /**
     * Check if coupon code exists
     */
    boolean existsByCouponCode(String couponCode);
    
    /**
     * Find coupon by case ID
     */
    Optional<SupervisorCoupon> findByCaseId(Long caseId);
    
    /**
     * Get total coupon value by supervisor
     */
    @Query("SELECT SUM(c.amount) FROM SupervisorCoupon c WHERE c.supervisor.id = :supervisorId " +
           "AND c.status = 'AVAILABLE' AND c.isDeleted = false AND c.expiresAt > :now")
    Double getTotalAvailableValueBySupervisor(
            @Param("supervisorId") Long supervisorId,
            @Param("now") LocalDateTime now);
    
    /**
     * Get coupon statistics for a supervisor
     */
//    @Query("SELECT " +
//           "COUNT(CASE WHEN c.status = 'AVAILABLE' AND c.expiresAt > :now THEN 1 END) as available, " +
//           "COUNT(CASE WHEN c.status = 'USED' THEN 1 END) as used, " +
//           "COUNT(CASE WHEN c.status = 'EXPIRED' OR (c.status = 'AVAILABLE' AND c.expiresAt < :now) THEN 1 END) as expired, " +
//           "COUNT(CASE WHEN c.status = 'CANCELLED' THEN 1 END) as cancelled " +
//           "FROM SupervisorCoupon c WHERE c.supervisor.id = :supervisorId AND c.isDeleted = false")
//    Object[] getCouponStatistics(@Param("supervisorId") Long supervisorId, @Param("now") LocalDateTime now);

    @Query("SELECT " +
            "COUNT(CASE WHEN c.status = 'AVAILABLE' AND c.expiresAt > :now THEN 1 END) as available, " +
            "COUNT(CASE WHEN c.status = 'USED' THEN 1 END) as used, " +
            "COUNT(CASE WHEN c.status = 'EXPIRED' OR (c.status = 'AVAILABLE' AND c.expiresAt < :now) THEN 1 END) as expired, " +
            "COUNT(CASE WHEN c.status = 'CANCELLED' THEN 1 END) as cancelled " +
            "FROM SupervisorCoupon c WHERE c.supervisor.id = :supervisorId AND c.isDeleted = false")
    CouponStatistics getCouponStatistics(@Param("supervisorId") Long supervisorId, @Param("now") LocalDateTime now);

    public interface CouponStatistics {
        Long getAvailable();
        Long getUsed();
        Long getExpired();
        Long getCancelled();
    }

    /**
     * Find coupon by code and supervisor
     */
    Optional<SupervisorCoupon> findByCouponCodeAndSupervisorIdAndIsDeletedFalse(
            String couponCode, Long supervisorId);

    /**
     * Find available coupon by code, supervisor, and patient
     */
    @Query("SELECT c FROM SupervisorCoupon c WHERE " +
            "c.couponCode = :couponCode AND " +
            "c.supervisor.id = :supervisorId AND " +
            "(c.patientId = :patientId OR c.patientId IS NULL) AND " +
            "c.status = :status AND " +
            "c.isDeleted = false AND " +
            "(c.expiresAt IS NULL OR c.expiresAt > CURRENT_TIMESTAMP)")
    Optional<SupervisorCoupon> findAvailableCoupon(
            @Param("couponCode") String couponCode,
            @Param("supervisorId") Long supervisorId,
            @Param("patientId") Long patientId,
            @Param("status") CouponStatus status);

    /**
     * Find all coupons for a supervisor
     */
    List<SupervisorCoupon> findBySupervisorIdAndIsDeletedFalseOrderByCreatedAtDesc(Long supervisorId);

    /**
     * Find coupons by supervisor and status
     */
    List<SupervisorCoupon> findBySupervisorIdAndStatusAndIsDeletedFalseOrderByExpiresAtAsc(
            Long supervisorId, CouponStatus status);

    /**
     * Find available coupons for a specific patient
     */
    @Query("SELECT c FROM SupervisorCoupon c WHERE " +
            "c.supervisor.id = :supervisorId AND " +
            "(c.patientId = :patientId OR c.patientId IS NULL) AND " +
            "c.status = 'AVAILABLE' AND " +
            "c.isDeleted = false AND " +
            "(c.expiresAt IS NULL OR c.expiresAt > CURRENT_TIMESTAMP) " +
            "ORDER BY c.expiresAt ASC NULLS LAST")
    List<SupervisorCoupon> findAvailableCouponsForPatient(
            @Param("supervisorId") Long supervisorId,
            @Param("patientId") Long patientId);

    /**
     * Find all available coupons for supervisor
     */
    @Query("SELECT c FROM SupervisorCoupon c WHERE " +
            "c.supervisor.id = :supervisorId AND " +
            "c.status = 'AVAILABLE' AND " +
            "c.isDeleted = false AND " +
            "(c.expiresAt IS NULL OR c.expiresAt > CURRENT_TIMESTAMP) " +
            "ORDER BY c.expiresAt ASC NULLS LAST")
    List<SupervisorCoupon> findAllAvailableCoupons(@Param("supervisorId") Long supervisorId);

    /**
     * Find coupons expiring soon (within given days)
     */
    @Query("SELECT c FROM SupervisorCoupon c WHERE " +
            "c.supervisor.id = :supervisorId AND " +
            "c.status = 'AVAILABLE' AND " +
            "c.isDeleted = false AND " +
            "c.expiresAt IS NOT NULL AND " +
            "c.expiresAt > CURRENT_TIMESTAMP AND " +
            "c.expiresAt <= :expiryThreshold " +
            "ORDER BY c.expiresAt ASC")
    List<SupervisorCoupon> findCouponsExpiringSoon(
            @Param("supervisorId") Long supervisorId,
            @Param("expiryThreshold") LocalDateTime expiryThreshold);

    /**
     * Count coupons by supervisor and status
     */
    long countBySupervisorIdAndStatusAndIsDeletedFalse(Long supervisorId, CouponStatus status);

    /**
     * Count available coupons for supervisor
     */
    @Query("SELECT COUNT(c) FROM SupervisorCoupon c WHERE " +
            "c.supervisor.id = :supervisorId AND " +
            "c.status = 'AVAILABLE' AND " +
            "c.isDeleted = false AND " +
            "(c.expiresAt IS NULL OR c.expiresAt > CURRENT_TIMESTAMP)")
    long countAvailableCoupons(@Param("supervisorId") Long supervisorId);

    /**
     * Get total value of available coupons
     */
    @Query("SELECT COALESCE(SUM(c.discountValue), 0) FROM SupervisorCoupon c WHERE " +
            "c.supervisor.id = :supervisorId AND " +
            "c.status = 'AVAILABLE' AND " +
            "c.discountType = 'FIXED_AMOUNT' AND " +
            "c.isDeleted = false AND " +
            "(c.expiresAt IS NULL OR c.expiresAt > CURRENT_TIMESTAMP)")
    java.math.BigDecimal getTotalAvailableValue(@Param("supervisorId") Long supervisorId);

    /**
     * Mark expired coupons
     */
    @Modifying
    @Query("UPDATE SupervisorCoupon c SET c.status = 'EXPIRED' WHERE " +
            "c.status = 'AVAILABLE' AND " +
            "c.expiresAt IS NOT NULL AND " +
            "c.expiresAt < CURRENT_TIMESTAMP")
    int markExpiredCoupons();

    /**
     * Find coupons by batch ID
     */
    List<SupervisorCoupon> findByBatchIdAndIsDeletedFalse(Long batchId);

    /**
     * Find used coupons for case
     */
    Optional<SupervisorCoupon> findByUsedForCaseIdAndStatusAndIsDeletedFalse(
            Long caseId, CouponStatus status);

    /**
     * Count total coupons (not deleted)
     */
    @Query("SELECT COUNT(c) FROM SupervisorCoupon c WHERE c.isDeleted = false")
    Long countTotalCoupons();

    /**
     * Count coupons by status (globally)
     */
    long countByStatusAndIsDeletedFalse(CouponStatus status);

    /**
     * Get coupon status breakdown
     */
    @Query("SELECT c.status, COUNT(c) FROM SupervisorCoupon c " +
            "WHERE c.isDeleted = false GROUP BY c.status")
    List<Object[]> getCouponStatusStatistics();

    /**
     * Count coupons expiring soon (globally, within 7 days)
     */
    @Query("SELECT COUNT(c) FROM SupervisorCoupon c WHERE " +
            "c.status = 'AVAILABLE' AND c.isDeleted = false AND " +
            "c.expiresAt IS NOT NULL AND c.expiresAt > CURRENT_TIMESTAMP AND " +
            "c.expiresAt <= :threshold")
    Long countCouponsExpiringSoon(@Param("threshold") LocalDateTime threshold);

    /**
     * Get total available coupon value (globally)
     */
    @Query("SELECT COALESCE(SUM(c.discountValue), 0) FROM SupervisorCoupon c WHERE " +
            "c.status = 'AVAILABLE' AND c.discountType = 'FIXED_AMOUNT' AND " +
            "c.isDeleted = false AND (c.expiresAt IS NULL OR c.expiresAt > CURRENT_TIMESTAMP)")
    java.math.BigDecimal getGlobalTotalAvailableValue();
}