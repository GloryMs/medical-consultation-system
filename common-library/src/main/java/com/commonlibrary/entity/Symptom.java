package com.commonlibrary.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.Set;

@Entity
@Table(name = "symptoms")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Symptom extends BaseEntity {
    
    @Column(nullable = false)
    private String code;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private String bodySystem; // Cardiovascular, Respiratory, etc.
    
    @ElementCollection
    @CollectionTable(name = "symptom_related_diseases", 
                    joinColumns = @JoinColumn(name = "symptom_id"))
    @Column(name = "disease_code")
    private Set<String> relatedDiseases;
    
    @ElementCollection
    @CollectionTable(name = "symptom_specializations", 
                    joinColumns = @JoinColumn(name = "symptom_id"))
    @Column(name = "specialization")
    private Set<String> relevantSpecializations;
    
    @Column(nullable = false)
    private Boolean isActive = true;
}