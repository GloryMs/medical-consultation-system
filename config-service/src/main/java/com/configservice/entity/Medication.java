package com.configservice.entity;

import com.commonlibrary.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "medications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Medication extends BaseEntity {
    
    @Column(unique = true, nullable = false)
    private String atcCode; // Anatomical Therapeutic Chemical code
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String genericName;
    
    @Column(nullable = false)
    private String category; // Therapeutic category
    
    private String subCategory;
    
    @ElementCollection
    @CollectionTable(name = "medication_indications", 
                    joinColumns = @JoinColumn(name = "medication_id"))
    @Column(name = "indication")
    private Set<String> indications; // What diseases this treats
    
    @ElementCollection
    @CollectionTable(name = "medication_contraindications", 
                    joinColumns = @JoinColumn(name = "medication_id"))
    @Column(name = "contraindication")
    private Set<String> contraindications;
    
    @Column(nullable = false)
    private Boolean isActive = true;
}