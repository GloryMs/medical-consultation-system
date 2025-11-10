package com.paymentservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PayoutResponseDto {

    private String transferId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime processedAt;
    private String message;
}
