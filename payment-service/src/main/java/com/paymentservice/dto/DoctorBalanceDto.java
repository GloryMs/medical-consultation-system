package com.paymentservice.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class DoctorBalanceDto {
    private Long doctorId;
    private BigDecimal availableBalance;
    private BigDecimal pendingBalance;
    private BigDecimal totalEarned;
    private BigDecimal totalWithdrawn;
    private BigDecimal totalRefunded;
    private BigDecimal totalFeesPaid;
    private boolean canWithdraw;
    private BigDecimal minimumPayoutAmount;
    private LocalDateTime lastWithdrawalAt;
}
