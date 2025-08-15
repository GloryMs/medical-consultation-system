package com.paymentservice.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentReceiptDto {
    private String receiptNumber;
    private Long paymentId;
    private String transactionId;
    private LocalDateTime paymentDate;
    private String paymentType;
    private BigDecimal amount;
    private BigDecimal platformFee;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String status;
    private String currency;
    private String payerName;
    private String payeeName;
}