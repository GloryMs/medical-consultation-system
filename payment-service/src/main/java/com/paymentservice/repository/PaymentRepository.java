package com.paymentservice.repository;

import com.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByPatientId(Long patientId);
    List<Payment> findByDoctorId(Long doctorId);
    List<Payment> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<Payment> findByProcessedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED'")
    Double calculateTotalRevenue();

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED' AND p.processedAt BETWEEN :start AND :end")
    Double calculateRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    List<Payment> findByPaymentType(String paymentType);
}
