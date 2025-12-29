package com.paymentservice.repository;

import com.paymentservice.entity.RefundLog;
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

@Repository
public interface RefundLogRepository extends JpaRepository<RefundLog, Long> {

    List<RefundLog> findByPaymentId(Long paymentId);

    List<RefundLog> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);

    Optional<RefundLog> findByStripeRefundId(String stripeRefundId);

    List<RefundLog> findByStatus(String status);

    List<RefundLog> findByInitiatedBy(Long initiatedBy);

    List<RefundLog> findByProcessedAtBetween(LocalDateTime start, LocalDateTime end);

    Page<RefundLog> findByStatus(String status, Pageable pageable);

    @Query("SELECT SUM(r.refundAmount) FROM RefundLog r WHERE r.status = 'COMPLETED' AND r.processedAt BETWEEN :start AND :end")
    BigDecimal getTotalRefundedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(r) FROM RefundLog r WHERE r.status = :status AND r.processedAt >= :since")
    Long countRefundsSince(@Param("status") String status, @Param("since") LocalDateTime since);

    @Query("SELECT r FROM RefundLog r WHERE r.status = 'PENDING' ORDER BY r.createdAt ASC")
    List<RefundLog> findPendingRefunds();

    // =============== REFUND ANALYTICS QUERIES ===============

    // Refund Reason Statistics (by refundType)
    @Query("SELECT r.refundType, COUNT(r), SUM(r.refundAmount) FROM RefundLog r " +
           "WHERE r.status = 'COMPLETED' AND r.processedAt BETWEEN :start AND :end " +
           "GROUP BY r.refundType")
    List<Object[]> getRefundReasonStats(@Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    // Daily Refund Trend
    @Query("SELECT CAST(r.processedAt AS DATE), COUNT(r), SUM(r.refundAmount) FROM RefundLog r " +
           "WHERE r.status = 'COMPLETED' AND r.processedAt BETWEEN :start AND :end " +
           "GROUP BY CAST(r.processedAt AS DATE) " +
           "ORDER BY CAST(r.processedAt AS DATE)")
    List<Object[]> getDailyRefundTrend(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    // Count Completed Refunds in Date Range
    @Query("SELECT COUNT(r) FROM RefundLog r " +
           "WHERE r.status = 'COMPLETED' AND r.processedAt BETWEEN :start AND :end")
    Long countCompletedRefunds(@Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

    // Total Refunded Amount in Date Range
    @Query("SELECT SUM(r.refundAmount) FROM RefundLog r " +
           "WHERE r.status = 'COMPLETED' AND r.processedAt BETWEEN :start AND :end")
    BigDecimal getTotalRefundedAmount(@Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    // Average Refund Amount
    @Query("SELECT AVG(r.refundAmount) FROM RefundLog r " +
           "WHERE r.status = 'COMPLETED' AND r.processedAt BETWEEN :start AND :end")
    BigDecimal getAverageRefundAmount(@Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    // Refunds by Payment Type (requires join with Payment table)
    @Query("SELECT p.paymentType, COUNT(r) FROM RefundLog r " +
           "JOIN Payment p ON r.paymentId = p.id " +
           "WHERE r.status = 'COMPLETED' AND r.processedAt BETWEEN :start AND :end " +
           "GROUP BY p.paymentType")
    List<Object[]> getRefundsByPaymentType(@Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    // Count all refunds (any status) in date range
    @Query("SELECT COUNT(r) FROM RefundLog r WHERE r.processedAt BETWEEN :start AND :end")
    Long countAllRefundsInRange(@Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);
}