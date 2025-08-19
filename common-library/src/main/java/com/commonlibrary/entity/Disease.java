package com.commonlibrary.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "diseases")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Disease extends BaseEntity {
    
    @Column(unique = true, nullable = false)
    private String icdCode; // ICD-10 code
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private String category; // Primary medical category
    
    private String subCategory;
    
    @ElementCollection
    @CollectionTable(name = "disease_specializations",
                    joinColumns = @JoinColumn(name = "disease_id"))
    @Column(name = "specialization")
    private Set<String> requiredSpecializations;
    
    @ElementCollection
    @CollectionTable(name = "disease_symptoms", 
                    joinColumns = @JoinColumn(name = "disease_id"))
    @Column(name = "symptom")
    private Set<String> commonSymptoms;
    
    @Enumerated(EnumType.STRING)
    private DiseaseSeverity defaultSeverity;
    
    @Column(nullable = false)
    private Boolean isActive = true;
}