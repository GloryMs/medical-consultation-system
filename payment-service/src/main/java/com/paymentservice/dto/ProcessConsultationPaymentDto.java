package com.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessConsultationPaymentDto {
    private Long patientId;
    private Long doctorId;
    private Long caseId;
    private Long appointmentId;
    private String paymentMethodId;
}