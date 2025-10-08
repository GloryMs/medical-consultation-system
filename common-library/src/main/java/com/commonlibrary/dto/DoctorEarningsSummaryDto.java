package com.commonlibrary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorEarningsSummaryDto {
    private BigDecimal totalEarnings;
    private BigDecimal monthlyEarnings;
    private BigDecimal weeklyEarnings;
    private BigDecimal todayEarnings;
    private BigDecimal pendingPayouts;
    private Long completedConsultations;
    private BigDecimal averageConsultationFee;
    private BigDecimal platformFeesDeducted;
    private String period; // week, month, year, all
}