package com.adminservice.repository;

import com.adminservice.entity.AdminCouponBatch;
import com.commonlibrary.entity.BeneficiaryType;
import com.commonlibrary.entity.CouponBatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for AdminCouponBatch entity.
 * Provides query methods for batch coupon management.
 */
@Repository
public interface AdminCouponBatchRepository extends JpaRepository<AdminCouponBatch, Long> {

    // ==================== Basic Lookups ====================

    /**
     * Find batch by code
     */
    Optional<AdminCouponBatch> findByBatchCodeAndIsDeletedFalse(String batchCode);

    /**
     * Check if batch code exists
     */
    boolean existsByBatchCode(String batchCode);

    // ==================== Status-based Queries ====================

    /**
     * Find batches by status
     */
    List<AdminCouponBatch> findByStatusAndIsDeletedFalse(CouponBatchStatus status);

    /**
     * Find batches by status with pagination
     */
    Page<AdminCouponBatch> findByStatusAndIsDeletedFalse(CouponBatchStatus status, Pageable pageable);

    /**
     * Find batches by multiple statuses
     */
    List<AdminCouponBatch> findByStatusInAndIsDeletedFalse(List<CouponBatchStatus> statuses);

    // ==================== Beneficiary-based Queries ====================

    /**
     * Find batches by beneficiary type and ID
     */
    List<AdminCouponBatch> findByBeneficiaryTypeAndBeneficiaryIdAndIsDeletedFalse(
            BeneficiaryType beneficiaryType, Long beneficiaryId);

    /**
     * Find batches by beneficiary type and ID with pagination
     */
    Page<AdminCouponBatch> findByBeneficiaryTypeAndBeneficiaryIdAndIsDeletedFalse(
            BeneficiaryType beneficiaryType, Long beneficiaryId, Pageable pageable);

    /**
     * Find batches by beneficiary type only
     */
    List<AdminCouponBatch> findByBeneficiaryTypeAndIsDeletedFalse(BeneficiaryType beneficiaryType);

    // ==================== Admin Tracking ====================

    /**
     * Find batches created by admin
     */
    List<AdminCouponBatch> findByCreatedByAndIsDeletedFalse(Long createdBy);

    /**
     * Find batches created by admin with pagination
     */
    Page<AdminCouponBatch> findByCreatedByAndIsDeletedFalse(Long createdBy, Pageable pageable);

    // ==================== Statistics Queries ====================

    /**
     * Count batches by status
     */
    long countByStatusAndIsDeletedFalse(CouponBatchStatus status);

    /**
     * Count batches by beneficiary
     */
    long countByBeneficiaryTypeAndBeneficiaryIdAndIsDeletedFalse(
            BeneficiaryType beneficiaryType, Long beneficiaryId);

    /**
     * Get total coupons created via batches
     */
    @Query("SELECT COALESCE(SUM(b.totalCoupons), 0) FROM AdminCouponBatch b " +
           "WHERE b.isDeleted = false")
    long getTotalCouponsFromBatches();

    /**
     * Get batch statistics by status
     */
    @Query("SELECT b.status, COUNT(b), SUM(b.totalCoupons) FROM AdminCouponBatch b " +
           "WHERE b.isDeleted = false GROUP BY b.status")
    List<Object[]> getBatchStatsByStatus();

    // ==================== Search & Filter ====================

    /**
     * Search batches with multiple filters
     */
    @Query("SELECT b FROM AdminCouponBatch b WHERE b.isDeleted = false " +
           "AND (:status IS NULL OR b.status = :status) " +
           "AND (:beneficiaryType IS NULL OR b.beneficiaryType = :beneficiaryType) " +
           "AND (:beneficiaryId IS NULL OR b.beneficiaryId = :beneficiaryId) " +
           "AND (:batchCode IS NULL OR b.batchCode LIKE %:batchCode%)")
    Page<AdminCouponBatch> searchBatches(
            @Param("status") CouponBatchStatus status,
            @Param("beneficiaryType") BeneficiaryType beneficiaryType,
            @Param("beneficiaryId") Long beneficiaryId,
            @Param("batchCode") String batchCode,
            Pageable pageable);

    // ==================== Update Operations ====================

    /**
     * Update batch status
     */
    @Modifying
    @Query("UPDATE AdminCouponBatch b SET b.status = :status, b.updatedAt = :now " +
           "WHERE b.id = :batchId AND b.isDeleted = false")
    int updateBatchStatus(@Param("batchId") Long batchId,
                          @Param("status") CouponBatchStatus status,
                          @Param("now") LocalDateTime now);

    /**
     * Update batch distribution info
     */
    @Modifying
    @Query("UPDATE AdminCouponBatch b SET b.status = 'DISTRIBUTED', " +
           "b.beneficiaryId = :beneficiaryId, " +
           "b.distributedBy = :distributedBy, " +
           "b.distributedAt = :distributedAt, " +
           "b.updatedAt = :distributedAt " +
           "WHERE b.id = :batchId " +
           "AND b.status = 'CREATED' " +
           "AND b.isDeleted = false")
    int distributeBatch(@Param("batchId") Long batchId,
                        @Param("beneficiaryId") Long beneficiaryId,
                        @Param("distributedBy") Long distributedBy,
                        @Param("distributedAt") LocalDateTime distributedAt);

    // ==================== Recent Batches ====================

    /**
     * Find recent batches
     */
    @Query("SELECT b FROM AdminCouponBatch b WHERE b.isDeleted = false " +
           "ORDER BY b.createdAt DESC")
    List<AdminCouponBatch> findRecentBatches(Pageable pageable);

    /**
     * Find recent batches for beneficiary
     */
    @Query("SELECT b FROM AdminCouponBatch b " +
           "WHERE b.beneficiaryType = :type " +
           "AND b.beneficiaryId = :beneficiaryId " +
           "AND b.isDeleted = false " +
           "ORDER BY b.createdAt DESC")
    List<AdminCouponBatch> findRecentBatchesForBeneficiary(
            @Param("type") BeneficiaryType type,
            @Param("beneficiaryId") Long beneficiaryId,
            Pageable pageable);
}