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
public class MedicationDto{

    private String atcCode;
    private String name;
    private String genericName;
    private String category;
    private String subCategory;
    private Set<String> indications;
    private Set<String> contraindications;
    private Boolean isActive;
}