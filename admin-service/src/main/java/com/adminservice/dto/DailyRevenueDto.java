package com.adminservice.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DailyRevenueDto {
    private LocalDate date;
    private Double revenue;
}