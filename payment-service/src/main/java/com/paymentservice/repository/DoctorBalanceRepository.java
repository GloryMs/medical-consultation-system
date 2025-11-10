package com.paymentservice.repository;

import com.paymentservice.entity.DoctorBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorBalanceRepository extends JpaRepository<DoctorBalance, Long> {

    Optional<DoctorBalance> findByDoctorId(Long doctorId);

    Optional<DoctorBalance> findByStripeConnectAccountId(String stripeConnectAccountId);

    List<DoctorBalance> findByPayoutEnabledTrue();

    List<DoctorBalance> findByBankAccountVerifiedTrue();

    @Query("SELECT db FROM DoctorBalance db WHERE db.availableBalance >= :minAmount")
    List<DoctorBalance> findByAvailableBalanceGreaterThanEqual(@Param("minAmount") BigDecimal minAmount);

    @Query("SELECT SUM(db.availableBalance) FROM DoctorBalance db")
    BigDecimal getTotalAvailableBalance();

    @Query("SELECT SUM(db.pendingBalance) FROM DoctorBalance db")
    BigDecimal getTotalPendingBalance();

    @Query("SELECT SUM(db.totalEarned) FROM DoctorBalance db")
    BigDecimal getTotalEarnedByAllDoctors();

    @Query("SELECT SUM(db.totalWithdrawn) FROM DoctorBalance db")
    BigDecimal getTotalWithdrawnByAllDoctors();

    @Query("SELECT db FROM DoctorBalance db WHERE db.availableBalance >= db.minimumPayoutAmount AND db.payoutEnabled = true")
    List<DoctorBalance> findEligibleForPayout();
}