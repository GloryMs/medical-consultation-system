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
}