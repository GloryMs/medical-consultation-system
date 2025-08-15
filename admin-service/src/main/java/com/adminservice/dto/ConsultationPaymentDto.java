package com.adminservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConsultationPaymentDto {
    private Long patientId;
    private Long doctorId;
    private Long caseId;
    private Double amount;
    private String status;
    private LocalDateTime paymentDate;
}