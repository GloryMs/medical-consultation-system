package com.supervisorservice.repository;

import com.supervisorservice.entity.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for CouponRedemption entity
 */
@Repository
public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {
    
    /**
     * Find redemption by coupon ID
     */
    Optional<CouponRedemption> findByCouponId(Long couponId);
    
    /**
     * Find redemption by payment ID
     */
    Optional<CouponRedemption> findByPaymentId(Long paymentId);
    
    /**
     * Find redemption by case ID
     */
    Optional<CouponRedemption> findByCaseId(Long caseId);
    
    /**
     * Find all redemptions for a supervisor
     */
    @Query("SELECT r FROM CouponRedemption r WHERE r.supervisor.id = :supervisorId " +
           "ORDER BY r.redeemedAt DESC")
    List<CouponRedemption> findBySupervisorId(@Param("supervisorId") Long supervisorId);
    
    /**
     * Find all redemptions for a patient
     */
    @Query("SELECT r FROM CouponRedemption r WHERE r.patientId = :patientId " +
           "ORDER BY r.redeemedAt DESC")
    List<CouponRedemption> findByPatientId(@Param("patientId") Long patientId);
    
    /**
     * Find redemptions within date range
     */
    @Query("SELECT r FROM CouponRedemption r WHERE r.redeemedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY r.redeemedAt DESC")
    List<CouponRedemption> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find redemptions for a supervisor within date range
     */
    @Query("SELECT r FROM CouponRedemption r WHERE r.supervisor.id = :supervisorId " +
           "AND r.redeemedAt BETWEEN :startDate AND :endDate ORDER BY r.redeemedAt DESC")
    List<CouponRedemption> findBySupervisorIdAndDateRange(
            @Param("supervisorId") Long supervisorId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    /**
     * Count redemptions by supervisor
     */
    @Query("SELECT COUNT(r) FROM CouponRedemption r WHERE r.supervisor.id = :supervisorId")
    Long countBySupervisorId(@Param("supervisorId") Long supervisorId);
    
    /**
     * Get total redemption value by supervisor
     */
    @Query("SELECT SUM(r.amount) FROM CouponRedemption r WHERE r.supervisor.id = :supervisorId")
    Double getTotalRedemptionValueBySupervisor(@Param("supervisorId") Long supervisorId);
    
    /**
     * Get redemption statistics for a supervisor
     */
    @Query("SELECT COUNT(r), SUM(r.amount), AVG(r.amount), MIN(r.amount), MAX(r.amount) " +
           "FROM CouponRedemption r WHERE r.supervisor.id = :supervisorId")
    Object[] getRedemptionStatistics(@Param("supervisorId") Long supervisorId);
    
    /**
     * Find recent redemptions
     */
    @Query("SELECT r FROM CouponRedemption r ORDER BY r.redeemedAt DESC")
    List<CouponRedemption> findRecentRedemptions();
}