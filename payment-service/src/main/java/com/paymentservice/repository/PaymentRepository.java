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
    List<Payment> findByPaymentType(PaymentType paymentType);
    List<Payment> findByPaymentTypeAndStatus(PaymentType paymentType, PaymentStatus status);

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
}