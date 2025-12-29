package com.paymentservice.repository;

import com.commonlibrary.entity.PaymentStatus;
import com.commonlibrary.entity.PaymentType;
import com.paymentservice.entity.Payment;
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
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Find by various IDs
    Optional<Payment> findByStripePaymentIntentId(String paymentIntentId);
    Optional<Payment> findByStripeChargeId(String chargeId);
    Optional<Payment> findByTransactionId(String transactionId);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    // Find by patient, doctor, case
    List<Payment> findByPatientId(Long patientId);
    List<Payment> findByDoctorId(Long doctorId);
    List<Payment> findByCaseId(Long caseId);

    // Ordered queries
    List<Payment> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<Payment> findByDoctorIdOrderByProcessedAtDesc(Long doctorId);

    // Status-based queries
    List<Payment> findByStatus(PaymentStatus status);
    List<Payment> findByDoctorIdAndStatus(Long doctorId, PaymentStatus status);
    List<Payment> findByPatientIdAndStatus(Long patientId, PaymentStatus status);
    List<Payment> findByDoctorIdAndStatusOrderByProcessedAtDesc(Long doctorId, PaymentStatus status);

    // Date range queries
    List<Payment> findByProcessedAtBetween(LocalDateTime start, LocalDateTime end);
    List<Payment> findByDoctorIdAndProcessedAtBetween(Long doctorId, LocalDateTime start, LocalDateTime end);
    List<Payment> findByPatientIdAndProcessedAtBetween(Long patientId, LocalDateTime start, LocalDateTime end);

    // Payment type queries
    @Query("SELECT p FROM Payment p WHERE p.paymentType = :paymentType")
    List<Payment> findByPaymentType(@Param("paymentType") PaymentType paymentType);

    @Query("SELECT p FROM Payment p WHERE p.paymentType = :paymentType AND p.status = :status")
    List<Payment> findByPaymentTypeAndStatus(@Param("paymentType") PaymentType paymentType,
                                            @Param("status") PaymentStatus status);

    // Combined queries
    List<Payment> findByDoctorIdAndStatusAndProcessedAtBetween(
            Long doctorId, PaymentStatus status, LocalDateTime start, LocalDateTime end);

    // Pagination queries
    Page<Payment> findByPatientId(Long patientId, Pageable pageable);
    Page<Payment> findByDoctorId(Long doctorId, Pageable pageable);
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    // Aggregate queries
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = :status")
    BigDecimal calculateTotalRevenue(@Param("status") PaymentStatus status);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = :status AND p.processedAt BETWEEN :start AND :end")
    BigDecimal calculateRevenueBetween(@Param("status") PaymentStatus status,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    @Query("SELECT SUM(p.platformFee) FROM Payment p WHERE p.status = :status AND p.processedAt BETWEEN :start AND :end")
    BigDecimal calculatePlatformFeeBetween(@Param("status") PaymentStatus status,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    @Query("SELECT SUM(p.doctorAmount) FROM Payment p WHERE p.doctorId = :doctorId AND p.status = :status")
    BigDecimal calculateDoctorTotalEarnings(@Param("doctorId") Long doctorId,
                                            @Param("status") PaymentStatus status);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.doctorId = :doctorId AND p.status = :status")
    Long countCompletedPaymentsByDoctor(@Param("doctorId") Long doctorId,
                                        @Param("status") PaymentStatus status);

    @Query("SELECT AVG(p.doctorAmount) FROM Payment p WHERE p.doctorId = :doctorId AND p.status = :status")
    BigDecimal calculateDoctorAverageEarnings(@Param("doctorId") Long doctorId,
                                              @Param("status") PaymentStatus status);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status AND p.processedAt >= :since")
    Long countPaymentsSince(@Param("status") PaymentStatus status, @Param("since") LocalDateTime since);

    // Refund queries
    @Query("SELECT p FROM Payment p WHERE p.refundedAt IS NOT NULL ORDER BY p.refundedAt DESC")
    List<Payment> findRefundedPayments();

    @Query("SELECT SUM(p.refundAmount) FROM Payment p WHERE p.refundedAt BETWEEN :start AND :end")
    BigDecimal calculateTotalRefundsBetween(@Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    // =============== PAYMENT ANALYTICS QUERIES ===============

    // Total Revenue by Status and Date Range
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = :status AND p.processedAt BETWEEN :start AND :end")
    BigDecimal getTotalRevenue(@Param("status") PaymentStatus status,
                               @Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

    // Revenue by Type (SUBSCRIPTION, CONSULTATION)
    @Query("SELECT p.paymentType, SUM(p.amount) FROM Payment p " +
           "WHERE p.status = 'COMPLETED' AND p.processedAt BETWEEN :start AND :end " +
           "GROUP BY p.paymentType")
    List<Object[]> getRevenueByType(@Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    // Daily Revenue Trend
    @Query("SELECT CAST(p.processedAt AS DATE), SUM(p.amount) FROM Payment p " +
           "WHERE p.status = 'COMPLETED' AND p.processedAt BETWEEN :start AND :end " +
           "GROUP BY CAST(p.processedAt AS DATE) " +
           "ORDER BY CAST(p.processedAt AS DATE)")
    List<Object[]> getDailyRevenue(@Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    // Count by Status and Date Range
    @Query("SELECT COUNT(p) FROM Payment p " +
           "WHERE p.status = :status AND p.processedAt BETWEEN :start AND :end")
    Long countByStatusAndDateRange(@Param("status") PaymentStatus status,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    // Daily Transactions by Status
    @Query("SELECT CAST(p.processedAt AS DATE), p.status, COUNT(p) FROM Payment p " +
           "WHERE p.processedAt BETWEEN :start AND :end " +
           "GROUP BY CAST(p.processedAt AS DATE), p.status " +
           "ORDER BY CAST(p.processedAt AS DATE)")
    List<Object[]> getDailyTransactionsByStatus(@Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    // Hourly Distribution
    @Query("SELECT EXTRACT(HOUR FROM p.processedAt), COUNT(p) FROM Payment p " +
           "WHERE p.processedAt BETWEEN :start AND :end " +
           "GROUP BY EXTRACT(HOUR FROM p.processedAt) " +
           "ORDER BY EXTRACT(HOUR FROM p.processedAt)")
    List<Object[]> getHourlyDistribution(@Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    // Payment Method Statistics (count, sum, avg)
    @Query("SELECT p.paymentMethod, COUNT(p), SUM(p.amount), AVG(p.amount) FROM Payment p " +
           "WHERE p.processedAt BETWEEN :start AND :end " +
           "GROUP BY p.paymentMethod")
    List<Object[]> getPaymentMethodStats(@Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    // Payment Method Success Rates
    @Query("SELECT p.paymentMethod, " +
           "COUNT(CASE WHEN p.status = 'COMPLETED' THEN 1 END), " +
           "COUNT(p) FROM Payment p " +
           "WHERE p.processedAt BETWEEN :start AND :end " +
           "GROUP BY p.paymentMethod")
    List<Object[]> getPaymentMethodSuccessRates(@Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    // Day of Week Statistics (PostgreSQL: 0=Sunday, 1=Monday, ..., 6=Saturday)
    @Query("SELECT EXTRACT(DAY FROM p.processedAt), SUM(p.amount), COUNT(p) FROM Payment p " +
           "WHERE p.status = 'COMPLETED' AND p.processedAt BETWEEN :start AND :end " +
           "GROUP BY EXTRACT(DAY FROM p.processedAt) " +
           "ORDER BY EXTRACT(DAY FROM p.processedAt)")
    List<Object[]> getDayOfWeekStats(@Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    // Average Transaction Value
    @Query("SELECT AVG(p.amount) FROM Payment p " +
           "WHERE p.status = 'COMPLETED' AND p.processedAt BETWEEN :start AND :end")
    BigDecimal getAverageTransactionValue(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    // Total Payments in Date Range
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.processedAt BETWEEN :start AND :end")
    Long countTotalPaymentsInRange(@Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    // Status Distribution
    @Query("SELECT p.status, COUNT(p) FROM Payment p " +
           "WHERE p.processedAt BETWEEN :start AND :end " +
           "GROUP BY p.status")
    List<Object[]> getStatusDistribution(@Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    // Type Distribution
    @Query("SELECT p.paymentType, COUNT(p) FROM Payment p " +
           "WHERE p.processedAt BETWEEN :start AND :end " +
           "GROUP BY p.paymentType")
    List<Object[]> getTypeDistribution(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    // Revenue by Payment Method
    @Query("SELECT p.paymentMethod, SUM(p.amount) FROM Payment p " +
           "WHERE p.status = 'COMPLETED' AND p.processedAt BETWEEN :start AND :end " +
           "GROUP BY p.paymentMethod")
    List<Object[]> getRevenueByPaymentMethod(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    // Monthly Revenue (year, month, total revenue)
    @Query("SELECT EXTRACT(YEAR FROM p.processedAt), EXTRACT(MONTH FROM p.processedAt), SUM(p.amount) " +
           "FROM Payment p " +
           "WHERE p.status = 'COMPLETED' AND p.processedAt BETWEEN :start AND :end " +
           "GROUP BY EXTRACT(YEAR FROM p.processedAt), EXTRACT(MONTH FROM p.processedAt) " +
           "ORDER BY EXTRACT(YEAR FROM p.processedAt), EXTRACT(MONTH FROM p.processedAt)")
    List<Object[]> getMonthlyRevenue(@Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);
}