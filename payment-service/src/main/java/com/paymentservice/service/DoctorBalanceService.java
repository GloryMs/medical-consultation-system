package com.paymentservice.service;

import com.paymentservice.dto.DoctorBalanceDto;
import com.paymentservice.dto.PayoutRequestDto;
import com.paymentservice.dto.PayoutResponseDto;
import com.paymentservice.entity.DoctorBalance;
import com.paymentservice.entity.Payment;
import com.paymentservice.exception.StripePaymentException;
import com.paymentservice.repository.DoctorBalanceRepository;
import com.paymentservice.service.stripe.StripePaymentGateway;
import com.stripe.model.Transfer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing doctor balances and payouts
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorBalanceService {
    
    private final DoctorBalanceRepository balanceRepository;
    private final StripePaymentGateway stripePaymentGateway;
    
    /**
     * Get or create doctor balance
     */
    @Transactional
    public DoctorBalance getOrCreateBalance(Long doctorId) {
        return balanceRepository.findByDoctorId(doctorId)
            .orElseGet(() -> {
                DoctorBalance balance = DoctorBalance.builder()
                    .doctorId(doctorId)
                    .availableBalance(BigDecimal.ZERO)
                    .pendingBalance(BigDecimal.ZERO)
                    .totalEarned(BigDecimal.ZERO)
                    .totalWithdrawn(BigDecimal.ZERO)
                    .totalRefunded(BigDecimal.ZERO)
                    .totalFeesPaid(BigDecimal.ZERO)
                    .build();
                return balanceRepository.save(balance);
            });
    }
    
    /**
     * Add earnings from completed consultation
     */
    @Transactional
    public void addConsultationEarnings(Payment payment) {
        DoctorBalance balance = getOrCreateBalance(payment.getDoctorId());
        
        // Add to pending balance initially (becomes available after settlement period)
        balance.addToPendingBalance(payment.getDoctorAmount());
        
        // Record platform fee
        balance.recordFees(payment.getPlatformFee());
        
        balanceRepository.save(balance);
        
        log.info("Added {} to doctor {} pending balance from payment {}", 
                payment.getDoctorAmount(), payment.getDoctorId(), payment.getId());
    }
    
    /**
     * Move funds from pending to available (after settlement period)
     */
    @Transactional
    public void settlePendingFunds(Long doctorId, BigDecimal amount) {
        DoctorBalance balance = getOrCreateBalance(doctorId);
        balance.movePendingToAvailable(amount);
        balanceRepository.save(balance);
        
        log.info("Settled {} from pending to available for doctor {}", amount, doctorId);
    }
    
    /**
     * Deduct from balance (for refunds)
     */
    @Transactional
    public void deductFromBalance(Long doctorId, BigDecimal amount, String reason) {
        DoctorBalance balance = getOrCreateBalance(doctorId);
        
        // First try to deduct from pending balance
        if (balance.getPendingBalance().compareTo(amount) >= 0) {
            balance.setPendingBalance(balance.getPendingBalance().subtract(amount));
        } else {
            // Deduct remainder from available balance
            BigDecimal fromPending = balance.getPendingBalance();
            BigDecimal fromAvailable = amount.subtract(fromPending);
            
            balance.setPendingBalance(BigDecimal.ZERO);
            balance.deductFromAvailableBalance(fromAvailable);
        }
        
        balance.recordRefund(amount);
        balanceRepository.save(balance);
        
        log.info("Deducted {} from doctor {} balance for: {}", amount, doctorId, reason);
    }
    
    /**
     * Process payout to doctor's bank account
     */
    @Transactional
    public PayoutResponseDto processPayout(PayoutRequestDto payoutRequest) throws StripePaymentException {
        DoctorBalance balance = balanceRepository.findByDoctorId(payoutRequest.getDoctorId())
            .orElseThrow(() -> new RuntimeException("Doctor balance not found"));
        
        // Validate payout
        if (!balance.canWithdraw(payoutRequest.getAmount())) {
            throw new RuntimeException("Payout not allowed. Check balance and payout settings.");
        }
        
        // Create Stripe transfer
        Map<String, String> metadata = new HashMap<>();
        metadata.put("doctor_id", String.valueOf(payoutRequest.getDoctorId()));
        metadata.put("payout_type", "MANUAL");
        
        Transfer transfer = stripePaymentGateway.createTransfer(
            payoutRequest.getAmount(),
            balance.getStripeConnectAccountId(),
            "Doctor payout",
            metadata
        );
        
        // Update balance
        balance.recordWithdrawal(payoutRequest.getAmount());
        balanceRepository.save(balance);
        
        log.info("Processed payout of {} for doctor {}", payoutRequest.getAmount(), payoutRequest.getDoctorId());
        
        return PayoutResponseDto.builder()
            .transferId(transfer.getId())
            .amount(payoutRequest.getAmount())
            .status("COMPLETED")
            .processedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * Get doctor balance details
     */
    public DoctorBalanceDto getDoctorBalance(Long doctorId) {
        DoctorBalance balance = getOrCreateBalance(doctorId);
        
        return DoctorBalanceDto.builder()
            .doctorId(doctorId)
            .availableBalance(balance.getAvailableBalance())
            .pendingBalance(balance.getPendingBalance())
            .totalEarned(balance.getTotalEarned())
            .totalWithdrawn(balance.getTotalWithdrawn())
            .totalRefunded(balance.getTotalRefunded())
            .totalFeesPaid(balance.getTotalFeesPaid())
            .canWithdraw(balance.getAvailableBalance().compareTo(balance.getMinimumPayoutAmount()) >= 0)
            .minimumPayoutAmount(balance.getMinimumPayoutAmount())
            .lastWithdrawalAt(balance.getLastWithdrawalAt())
            .build();
    }
    
    /**
     * Get all doctor balances
     */
    public List<DoctorBalanceDto> getAllDoctorBalances() {
        return balanceRepository.findAll().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Get doctors eligible for payout
     */
    public List<DoctorBalance> getEligibleForPayout() {
        return balanceRepository.findEligibleForPayout();
    }
    
    /**
     * Enable payout for doctor
     */
    @Transactional
    public void enablePayout(Long doctorId, String stripeConnectAccountId) {
        DoctorBalance balance = getOrCreateBalance(doctorId);
        balance.setStripeConnectAccountId(stripeConnectAccountId);
        balance.setBankAccountVerified(true);
        balance.setPayoutEnabled(true);
        balanceRepository.save(balance);
        
        log.info("Enabled payout for doctor {} with Stripe account {}", doctorId, stripeConnectAccountId);
    }
    
    /**
     * Disable payout for doctor
     */
    @Transactional
    public void disablePayout(Long doctorId, String reason) {
        DoctorBalance balance = getOrCreateBalance(doctorId);
        balance.setPayoutEnabled(false);
        balanceRepository.save(balance);
        
        log.info("Disabled payout for doctor {}: {}", doctorId, reason);
    }
    
    /**
     * Get platform-wide balance statistics
     */
    public Map<String, BigDecimal> getPlatformBalanceStatistics() {
        Map<String, BigDecimal> stats = new HashMap<>();
        
        stats.put("totalAvailableBalance", balanceRepository.getTotalAvailableBalance());
        stats.put("totalPendingBalance", balanceRepository.getTotalPendingBalance());
        stats.put("totalEarned", balanceRepository.getTotalEarnedByAllDoctors());
        stats.put("totalWithdrawn", balanceRepository.getTotalWithdrawnByAllDoctors());
        
        return stats;
    }
    
    /**
     * Process automatic payouts for eligible doctors
     */
    @Transactional
    public void processAutomaticPayouts() {
        List<DoctorBalance> eligibleDoctors = getEligibleForPayout();
        
        for (DoctorBalance balance : eligibleDoctors) {
            try {
                PayoutRequestDto payoutRequest = PayoutRequestDto.builder()
                    .doctorId(balance.getDoctorId())
                    .amount(balance.getAvailableBalance())
                    .build();
                
                processPayout(payoutRequest);
                
            } catch (Exception e) {
                log.error("Failed to process automatic payout for doctor {}", balance.getDoctorId(), e);
            }
        }
    }
    
    /**
     * Update minimum payout amount for doctor
     */
    @Transactional
    public void updateMinimumPayoutAmount(Long doctorId, BigDecimal amount) {
        DoctorBalance balance = getOrCreateBalance(doctorId);
        balance.setMinimumPayoutAmount(amount);
        balanceRepository.save(balance);
        
        log.info("Updated minimum payout amount for doctor {} to {}", doctorId, amount);
    }
    
    private DoctorBalanceDto convertToDto(DoctorBalance balance) {
        return DoctorBalanceDto.builder()
            .doctorId(balance.getDoctorId())
            .availableBalance(balance.getAvailableBalance())
            .pendingBalance(balance.getPendingBalance())
            .totalEarned(balance.getTotalEarned())
            .totalWithdrawn(balance.getTotalWithdrawn())
            .totalRefunded(balance.getTotalRefunded())
            .totalFeesPaid(balance.getTotalFeesPaid())
            .canWithdraw(balance.canWithdraw(balance.getMinimumPayoutAmount()))
            .minimumPayoutAmount(balance.getMinimumPayoutAmount())
            .lastWithdrawalAt(balance.getLastWithdrawalAt())
            .build();
    }
}