package com.paymentservice.dto;

import com.commonlibrary.dto.DoctorEarningsSummaryDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarningsReportDto {
    private DoctorEarningsSummaryDto summary;
    private List<PaymentHistoryDto> payments;
    private Map<String, Object> statistics;
    private LocalDateTime generatedAt;
    private String period;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long doctorId;
    private String doctorName;
}