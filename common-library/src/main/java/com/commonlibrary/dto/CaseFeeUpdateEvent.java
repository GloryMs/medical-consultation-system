package com.commonlibrary.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CaseFeeUpdateEvent {
    private Long caseId;
    private Long doctorId;
    private Long doctorUserId;
    private Long patientId;
    private Long patientUserId;
    private BigDecimal consultationFee;
    private LocalDateTime feeSetAt;
    private String eventType; // "CASE_FEE_SET"
    private Long timestamp;
}