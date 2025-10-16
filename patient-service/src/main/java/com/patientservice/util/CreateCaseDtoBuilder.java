package com.patientservice.util;

import com.commonlibrary.entity.CaseComplexity;
import com.commonlibrary.entity.UrgencyLevel;
import com.patientservice.dto.CreateCaseDto;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for building CreateCaseDto from multipart form data
 */
@Component
public class CreateCaseDtoBuilder {

    /**
     * Build CreateCaseDto from individual form parameters
     */
    public CreateCaseDto buildCreateCaseDto(
            String caseTitle, 
            String description, 
            String primaryDiseaseCode, 
            List<String> secondaryDiseaseCodes,
            List<String> symptomCodes, 
            List<String> currentMedicationCodes,
            String requiredSpecialization, 
            List<String> secondarySpecializations,
            String urgencyLevel, 
            String complexity, 
            Boolean requiresSecondOpinion,
            Integer minDoctorsRequired, 
            Integer maxDoctorsAllowed,
            Long dependentId,
            List<MultipartFile> files) {
        
        CreateCaseDto dto = new CreateCaseDto();
        
        // Basic case information
        dto.setCaseTitle(caseTitle);
        dto.setDescription(description);
        dto.setPrimaryDiseaseCode(primaryDiseaseCode);
        
        // Convert lists to sets
        dto.setSecondaryDiseaseCodes(secondaryDiseaseCodes != null ? 
            secondaryDiseaseCodes.stream().collect(Collectors.toSet()) : null);
        dto.setSymptomCodes(symptomCodes != null ? 
            symptomCodes.stream().collect(Collectors.toSet()) : null);
        dto.setCurrentMedicationCodes(currentMedicationCodes != null ? 
            currentMedicationCodes.stream().collect(Collectors.toSet()) : null);
        dto.setSecondarySpecializations(secondarySpecializations != null ? 
            secondarySpecializations.stream().collect(Collectors.toSet()) : null);
        
        // Specialization
        dto.setRequiredSpecialization(requiredSpecialization);
        
        // Parse enums safely
        dto.setUrgencyLevel(parseUrgencyLevel(urgencyLevel));
        dto.setComplexity(parseCaseComplexity(complexity));
        
        // Optional fields with defaults
        dto.setRequiresSecondOpinion(requiresSecondOpinion != null ? requiresSecondOpinion : false);
        dto.setMinDoctorsRequired(minDoctorsRequired != null ? minDoctorsRequired : 1);
        dto.setMaxDoctorsAllowed(maxDoctorsAllowed != null ? maxDoctorsAllowed : 2);

        dto.setDependentId(dependentId);
        
        // Files
        dto.setFiles(files != null ? files : List.of());
        
        return dto;
    }

    /**
     * Parse UrgencyLevel enum safely
     */
    private UrgencyLevel parseUrgencyLevel(String urgencyLevel) {
        if (urgencyLevel == null || urgencyLevel.trim().isEmpty()) {
            return UrgencyLevel.MEDIUM; // Default value
        }
        
        try {
            return UrgencyLevel.valueOf(urgencyLevel.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UrgencyLevel.MEDIUM; // Fallback to default
        }
    }

    /**
     * Parse CaseComplexity enum safely
     */
    private CaseComplexity parseCaseComplexity(String complexity) {
        if (complexity == null || complexity.trim().isEmpty()) {
            return CaseComplexity.MODERATE; // Default value
        }
        
        try {
            return CaseComplexity.valueOf(complexity.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CaseComplexity.MODERATE; // Fallback to default
        }
    }

    /**
     * Build CreateCaseDto from JSON payload (for backward compatibility)
     */
    public CreateCaseDto buildFromJson(CreateCaseDto sourceDto) {
        CreateCaseDto dto = new CreateCaseDto();
        
        dto.setCaseTitle(sourceDto.getCaseTitle());
        dto.setDescription(sourceDto.getDescription());
        dto.setPrimaryDiseaseCode(sourceDto.getPrimaryDiseaseCode());
        dto.setSecondaryDiseaseCodes(sourceDto.getSecondaryDiseaseCodes());
        dto.setSymptomCodes(sourceDto.getSymptomCodes());
        dto.setCurrentMedicationCodes(sourceDto.getCurrentMedicationCodes());
        dto.setRequiredSpecialization(sourceDto.getRequiredSpecialization());
        dto.setSecondarySpecializations(sourceDto.getSecondarySpecializations());
        dto.setUrgencyLevel(sourceDto.getUrgencyLevel() != null ? sourceDto.getUrgencyLevel() : UrgencyLevel.MEDIUM);
        dto.setComplexity(sourceDto.getComplexity() != null ? sourceDto.getComplexity() : CaseComplexity.MODERATE);
        dto.setRequiresSecondOpinion(sourceDto.getRequiresSecondOpinion() != null ? sourceDto.getRequiresSecondOpinion() : false);
        dto.setMinDoctorsRequired(sourceDto.getMinDoctorsRequired() != null ? sourceDto.getMinDoctorsRequired() : 1);
        dto.setMaxDoctorsAllowed(sourceDto.getMaxDoctorsAllowed() != null ? sourceDto.getMaxDoctorsAllowed() : 2);
        dto.setDocumentIds(sourceDto.getDocumentIds());
        dto.setFiles(sourceDto.getFiles() != null ? sourceDto.getFiles() : List.of());
        
        return dto;
    }
}