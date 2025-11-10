package com.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DoctorBalance entity for tracking doctor earnings and withdrawals
 */
@Entity
@Table(name = "doctor_balances", indexes = {
        @Index(name = "idx_doctor_id", columnList = "doctor_id", unique = true),
        @Index(name = "idx_stripe_connect_account", columnList = "stripe_connect_account_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doctor_id", nullable = false, unique = true)
    private Long doctorId;

    @Column(name = "available_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "pending_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal pendingBalance = BigDecimal.ZERO;

    @Column(name = "total_earned", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalEarned = BigDecimal.ZERO;

    @Column(name = "total_withdrawn", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalWithdrawn = BigDecimal.ZERO;

    @Column(name = "total_refunded", precision = 12, scale = 2)
    private BigDecimal totalRefunded = BigDecimal.ZERO;

    @Column(name = "total_fees_paid", precision = 12, scale = 2)
    private BigDecimal totalFeesPaid = BigDecimal.ZERO; // Platform fees + Stripe fees on refunds

    @Column(name = "last_withdrawal_at")
    private LocalDateTime lastWithdrawalAt;

    @Column(name = "stripe_connect_account_id")
    private String stripeConnectAccountId;

    @Column(name = "bank_account_verified")
    private boolean bankAccountVerified = false;

    @Column(name = "payout_enabled")
    private boolean payoutEnabled = false;

    @Column(name = "minimum_payout_amount", precision = 10, scale = 2)
    private BigDecimal minimumPayoutAmount = new BigDecimal("50.00");

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (availableBalance == null) availableBalance = BigDecimal.ZERO;
        if (pendingBalance == null) pendingBalance = BigDecimal.ZERO;
        if (totalEarned == null) totalEarned = BigDecimal.ZERO;
        if (totalWithdrawn == null) totalWithdrawn = BigDecimal.ZERO;
        if (totalRefunded == null) totalRefunded = BigDecimal.ZERO;
        if (totalFeesPaid == null) totalFeesPaid = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public void addToAvailableBalance(BigDecimal amount) {
        this.availableBalance = this.availableBalance.add(amount);
        this.totalEarned = this.totalEarned.add(amount);
    }

    public void addToPendingBalance(BigDecimal amount) {
        this.pendingBalance = this.pendingBalance.add(amount);
    }

    public void movePendingToAvailable(BigDecimal amount) {
        if (this.pendingBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient pending balance");
        }
        this.pendingBalance = this.pendingBalance.subtract(amount);
        this.availableBalance = this.availableBalance.add(amount);
        this.totalEarned = this.totalEarned.add(amount);
    }

    public void deductFromAvailableBalance(BigDecimal amount) {
        if (this.availableBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient available balance");
        }
        this.availableBalance = this.availableBalance.subtract(amount);
    }

    public void recordWithdrawal(BigDecimal amount) {
        deductFromAvailableBalance(amount);
        this.totalWithdrawn = this.totalWithdrawn.add(amount);
        this.lastWithdrawalAt = LocalDateTime.now();
    }

    public void recordRefund(BigDecimal amount) {
        this.totalRefunded = this.totalRefunded.add(amount);
    }

    public void recordFees(BigDecimal fees) {
        this.totalFeesPaid = this.totalFeesPaid.add(fees);
    }

    public boolean canWithdraw(BigDecimal amount) {
        return payoutEnabled &&
                bankAccountVerified &&
                availableBalance.compareTo(amount) >= 0 &&
                amount.compareTo(minimumPayoutAmount) >= 0;
    }
}