package com.commonlibrary.dto;

import com.commonlibrary.entity.AssignmentPriority;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class DoctorMatchingResultDto {
    private DoctorDto doctor;
    private Double totalScore;
    private Map<String, Double> scoreBreakdown;
    private String matchingReason;
    private AssignmentPriority priority;
}