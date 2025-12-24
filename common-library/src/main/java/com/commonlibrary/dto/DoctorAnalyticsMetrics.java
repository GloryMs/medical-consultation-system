package com.commonlibrary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Doctor-specific analytics metrics
 * Performance analysis and workload distribution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorAnalyticsMetrics implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Top performing doctors (by resolution time, acceptance rate)
     */
    private List<DoctorPerformanceDto> topPerformers;
    
    /**
     * Bottom performing doctors (need attention)
     */
    private List<DoctorPerformanceDto> bottomPerformers;
    
    /**
     * Current workload distribution
     * Key: Doctor ID, Value: Current case count
     */
    private Map<Long, Integer> workloadDistribution;
    
    /**
     * Average acceptance rate across all doctors (%)
     */
    private Double avgAcceptanceRate;
    
    /**
     * Average rejection rate across all doctors (%)
     */
    private Double avgRejectionRate;
    
    /**
     * Total number of active doctors
     */
    private Integer totalActiveDoctors;
    
    /**
     * Total assignments made
     */
    private Long totalAssignments;
    
    /**
     * Cases by doctor name (for chart display)
     * Key: Doctor name, Value: Case count
     */
    private Map<String, Integer> casesByDoctor;
    
    /**
     * Average cases per doctor
     */
    private Double avgCasesPerDoctor;
    
    /**
     * Doctor utilization metrics
     */
    private DoctorUtilization utilization;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoctorUtilization implements Serializable {
        private Integer underutilizedCount;  // < 5 cases
        private Integer optimalCount;        // 5-15 cases
        private Integer overutilizedCount;   // > 15 cases
    }
}