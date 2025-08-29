package com.commonlibrary.dto;

import com.commonlibrary.entity.DiseaseSeverity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class DiseaseDto{

    private String icdCode;
    private String name;
    private String description;
    private String category;
    private String subCategory;
    private Set<String> requiredSpecializations;
    private Set<String> commonSymptoms;
    private DiseaseSeverity defaultSeverity;
    private Boolean isActive = true;
}