package com.supervisorservice.repository;

import com.supervisorservice.entity.SupervisorPayment;
import com.supervisorservice.entity.SupervisorPayment.PaymentMethodType;
import com.supervisorservice.entity.SupervisorPayment.SupervisorPaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupervisorPaymentRepository extends JpaRepository<SupervisorPayment, Long> {

    /**
     * Find payment by transaction ID
     */
    Optional<SupervisorPayment> findByTransactionId(String transactionId);

    /**
     * Find payment by case ID
     */
    Optional<SupervisorPayment> findByCaseIdAndStatus(Long caseId, SupervisorPaymentStatus status);

    /**
     * Find all payments for supervisor
     */
    List<SupervisorPayment> findBySupervisorIdOrderByCreatedAtDesc(Long supervisorId);

    /**
     * Find payments by supervisor and patient
     */
    List<SupervisorPayment> findBySupervisorIdAndPatientIdOrderByCreatedAtDesc(
            Long supervisorId, Long patientId);

    /**
     * Find payments by supervisor and status
     */
    List<SupervisorPayment> findBySupervisorIdAndStatusOrderByCreatedAtDesc(
            Long supervisorId, SupervisorPaymentStatus status);

    /**
     * Find payments by patient
     */
    List<SupervisorPayment> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    /**
     * Find payments by payment method
     */
    List<SupervisorPayment> findBySupervisorIdAndPaymentMethodOrderByCreatedAtDesc(
            Long supervisorId, PaymentMethodType paymentMethod);

    /**
     * Check if payment exists for case
     */
    boolean existsByCaseIdAndStatus(Long caseId, SupervisorPaymentStatus status);

    /**
     * Count payments by supervisor
     */
    long countBySupervisorId(Long supervisorId);

    /**
     * Count completed payments by supervisor
     */
    long countBySupervisorIdAndStatus(Long supervisorId, SupervisorPaymentStatus status);

    /**
     * Get total amount paid by supervisor
     */
    @Query("SELECT COALESCE(SUM(p.finalAmount), 0) FROM SupervisorPayment p WHERE " +
            "p.supervisorId = :supervisorId AND p.status = 'COMPLETED'")
    BigDecimal getTotalAmountPaid(@Param("supervisorId") Long supervisorId);

    /**
     * Get total amount paid for patient
     */
    @Query("SELECT COALESCE(SUM(p.finalAmount), 0) FROM SupervisorPayment p WHERE " +
            "p.supervisorId = :supervisorId AND p.patientId = :patientId AND p.status = 'COMPLETED'")
    BigDecimal getTotalAmountPaidForPatient(
            @Param("supervisorId") Long supervisorId,
            @Param("patientId") Long patientId);

    /**
     * Get total discount amount by supervisor
     */
    @Query("SELECT COALESCE(SUM(p.discountAmount), 0) FROM SupervisorPayment p WHERE " +
            "p.supervisorId = :supervisorId AND p.status = 'COMPLETED' AND p.paymentMethod = 'COUPON'")
    BigDecimal getTotalDiscountAmount(@Param("supervisorId") Long supervisorId);

    /**
     * Find payments in date range
     */
    @Query("SELECT p FROM SupervisorPayment p WHERE " +
            "p.supervisorId = :supervisorId AND " +
            "p.processedAt >= :startDate AND p.processedAt <= :endDate " +
            "ORDER BY p.processedAt DESC")
    List<SupervisorPayment> findPaymentsInDateRange(
            @Param("supervisorId") Long supervisorId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find by Stripe payment intent ID
     */
    Optional<SupervisorPayment> findByStripePaymentIntentId(String stripePaymentIntentId);

    /**
     * Find by PayPal order ID
     */
    Optional<SupervisorPayment> findByPaypalOrderId(String paypalOrderId);

    /**
     * Find pending payments older than threshold
     */
    @Query("SELECT p FROM SupervisorPayment p WHERE " +
            "p.status = 'PENDING' AND p.createdAt < :threshold")
    List<SupervisorPayment> findStalePendingPayments(@Param("threshold") LocalDateTime threshold);
}