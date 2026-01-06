package com.supervisorservice.repository;

import com.supervisorservice.entity.CouponBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CouponBatch entity
 */
@Repository
public interface CouponBatchRepository extends JpaRepository<CouponBatch, Long> {
    
    /**
     * Find batch by batch code
     */
    Optional<CouponBatch> findByBatchCode(String batchCode);
    
    /**
     * Find all batches for a supervisor
     */
    @Query("SELECT b FROM CouponBatch b WHERE b.supervisor.id = :supervisorId " +
           "ORDER BY b.createdAt DESC")
    List<CouponBatch> findBySupervisorId(@Param("supervisorId") Long supervisorId);
    
    /**
     * Find all batches for a patient
     */
    @Query("SELECT b FROM CouponBatch b WHERE b.patientId = :patientId " +
           "ORDER BY b.createdAt DESC")
    List<CouponBatch> findByPatientId(@Param("patientId") Long patientId);
    
    /**
     * Find batches for a supervisor and patient
     */
    @Query("SELECT b FROM CouponBatch b WHERE b.supervisor.id = :supervisorId " +
           "AND b.patientId = :patientId ORDER BY b.createdAt DESC")
    List<CouponBatch> findBySupervisorIdAndPatientId(
            @Param("supervisorId") Long supervisorId,
            @Param("patientId") Long patientId);
    
    /**
     * Check if batch code exists
     */
    boolean existsByBatchCode(String batchCode);
    
    /**
     * Get batch statistics for a supervisor
     */
    @Query("SELECT COUNT(b), SUM(b.totalCoupons), SUM(b.amountPerCoupon * b.totalCoupons) " +
           "FROM CouponBatch b WHERE b.supervisor.id = :supervisorId")
    Object[] getBatchStatistics(@Param("supervisorId") Long supervisorId);
    
    /**
     * Find recent batches
     */
    @Query("SELECT b FROM CouponBatch b ORDER BY b.createdAt DESC")
    List<CouponBatch> findRecentBatches();
}