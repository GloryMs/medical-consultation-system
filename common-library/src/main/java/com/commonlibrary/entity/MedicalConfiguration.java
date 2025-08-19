package com.commonlibrary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "medical_configurations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalConfiguration extends BaseEntity {
    
    @Column(nullable = false)
    private String configType; // DISEASE, SYMPTOM, MEDICATION, SPECIALIZATION, etc.
    
    @Column(nullable = false)
    private String code; // Unique identifier (e.g., ICD-10, ATC codes)
    
    @Column(nullable = false)
    private String name; // Display name
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private String parentCode; // For hierarchical structure
    
    private Integer level; // Hierarchy level (1=main category, 2=subcategory, etc.)
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    private Integer sortOrder;
    
    // Additional attributes as JSON
    @Column(columnDefinition = "TEXT")
    private String attributes; // JSON string for flexible attributes
}