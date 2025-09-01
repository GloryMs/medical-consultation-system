package com.commonlibrary.dto;

import com.commonlibrary.entity.DiseaseSeverity;
import lombok.Data;
import java.util.List;

@Data
public class DiseaseSearchCriteria {
    private String category;
    private String subCategory;
    private String requiredSpecialization;
    private List<String> symptoms;
    private DiseaseSeverity severity;
    private String nameContains;
    private Boolean isActive = true;
}