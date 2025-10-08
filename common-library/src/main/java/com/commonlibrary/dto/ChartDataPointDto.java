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
public class ChartDataPointDto {
    private String date;
    private String label;
    private BigDecimal earnings;
    private Integer count;
    private String paymentMethod;
    private BigDecimal value; // For pie chart
    private String name; // For pie chart
}