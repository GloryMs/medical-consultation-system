package com.commonlibrary.dto;

import com.commonlibrary.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SymptomDto extends BaseEntity {

    private String code;
    private String name;
    private String description;
    private String bodySystem;
    private Set<String> relatedDiseases;
    private Set<String> relevantSpecializations;
    private Boolean isActive;
}