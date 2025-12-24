package com.commonlibrary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Individual doctor performance metrics
 * Used for rankings and performance comparisons
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorPerformanceDto implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Doctor identification
     */
    private Long doctorId;
    private String doctorName;
    private String specialization;
    
    /**
     * Case volume metrics
     */
    private Integer totalAssignments;
    private Integer acceptedCases;
    private Integer rejectedCases;
    private Integer completedCases;
    private Integer currentLoad;
    
    /**
     * Performance metrics
     */
    private Double acceptanceRate;      // % of assignments accepted
    private Double rejectionRate;       // % of assignments rejected
    private Double avgResolutionTime;   // Average days to complete
    private Double avgResponseTime;     // Average hours to respond to assignment
    
    /**
     * Quality metrics
     */
    private Integer reassignmentCount;  // Cases reassigned away from this doctor
    private Double patientSatisfaction; // If available from ratings
    
    /**
     * Performance ranking
     */
    private Integer rank;               // Overall performance rank
    private String performanceLevel;    // "Excellent", "Good", "Average", "Needs Improvement"
}