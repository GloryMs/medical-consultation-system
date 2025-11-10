package com.paymentservice.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PayoutRequestDto {

    private Long doctorId;
    private BigDecimal amount;
    private String bankAccountId;
    private String description;
}
