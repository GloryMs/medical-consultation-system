package com.patientservice.service;

import com.commonlibrary.dto.CaseAnalysisRequest;
import com.commonlibrary.dto.DiseaseDto;
import com.commonlibrary.exception.BusinessException;
import com.patientservice.feign.MedicalConfigurationMainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalConfigurationService {

//    private final MedicalConfigurationRepository configRepository;
//    private final DiseaseRepository diseaseRepository;
//    private final MedicationRepository medicationRepository;
//    private final SymptomRepository symptomRepository;
    
    private final MedicalConfigurationMainService medicalConfigurationMainService;

    /*TODO 
    *  Re-enable all bellow functions*/
    
//    @Cacheable(value = "medical-configs", key = "#configType")
//    public List<MedicalConfigurationDto> getConfigurationsByType(String configType) {
//        return medicalConfigurationMainService.findByConfigTypeAndIsActiveTrueOrderBySortOrder(configType);
//    }
//
//    @Cacheable(value = "diseases")
//    public List<DiseaseDto> getAllActiveDiseases() {
//        return medicalConfigurationMainService.findByIsActiveTrueOrderByCategory();
//    }
//
//    @Cacheable(value = "diseases-by-category", key = "#category")
//    public List<DiseaseDto> getDiseasesByCategory(String category) {
//        return medicalConfigurationMainService.findByCategoryAndIsActiveTrueOrderByName(category);
//    }
//
//    @Cacheable(value = "medications")
//    public List<MedicationDto> getAllActiveMedications() {
//        return medicalConfigurationMainService.findByIsActiveTrueOrderByName();
//    }
//
//    @Cacheable(value = "medications-by-category", key = "#category")
//    public List<MedicationDto> getMedicationsByCategory(String category) {
//        return medicalConfigurationMainService.findByCategoryAndIsActiveTrueOrderByName(category);
//    }
//
//    @Cacheable(value = "symptoms")
//    public List<SymptomDto> getAllActiveSymptoms() {
//        
//        return medicalConfigurationMainService.findByIsActiveTrueOrderByBodySystem();
//    }
//
//    @Cacheable(value = "symptoms-by-system", key = "#bodySystem")
//    public List<SymptomDto> getSymptomsByBodySystem(String bodySystem) {
//        return medicalConfigurationMainService.findByBodySystemAndIsActiveTrueOrderByName(bodySystem);
//    }

//    public Set<String> getRelatedDiseasesForSymptoms(Set<String> symptomCodes) {
//        List<SymptomDto> symptoms = medicalConfigurationMainService.findByCodeInAndIsActiveTrue(symptomCodes);
//        return symptoms.stream()
//            .flatMap(symptom -> symptom.getRelatedDiseases().stream())
//            .collect(Collectors.toSet());
//    }
//    public boolean isDiseaseExisted(String icdCode) {
//        boolean isExsited = medicalConfigurationMainService.existsByIcdCode(icdCode);
//        return isExsited;
//    }

    public DiseaseDto getDiseaseByCode(String icdCode) {
        DiseaseDto disease = null;
        try{
            disease = medicalConfigurationMainService.getDiseaseByCode(icdCode);
        } catch (Exception e) {
            log.error("Disease not found: " + icdCode);
            log.error(e.getMessage());
        }
        return disease;
    }

    public DiseaseDto findDiseaseByIcdCodeCustom(String icdCode) {
        DiseaseDto disease;
        try{
            //disease = medicalConfigurationMainService.getSpecializationsForDisease(icdCode);
            disease = medicalConfigurationMainService.findDiseaseByIcdCodeCustom(icdCode);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException("Disease not found: " + icdCode, HttpStatus.NOT_FOUND);
        }

        return disease;
    }

    public List<String> getSpecializationsForDisease(String diseaseCode) {
        return medicalConfigurationMainService.getSpecializationsForDisease(diseaseCode);
    }
    
//
//    @CacheEvict(value = {"medical-configs", "diseases", "medications", "symptoms"}, allEntries = true)
//    public void clearCache() {
//        log.info("Medical configuration cache cleared");
//    }

    // ===== NEW METHODS FOR CASE ASSIGNMENT =====

    /**
     * Get diseases that can be handled by a specialization
     */
    @Cacheable(value = "specialization-diseases", key = "#specialization")
    public List<DiseaseDto> getDiseasesBySpecialization(String specialization) {
        try {
            return medicalConfigurationMainService.getDiseasesBySpecialization(specialization);
        } catch (Exception e) {
            log.error("Error getting diseases for specialization {}: {}", specialization, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Find compatible specializations for symptoms
     */
    @Cacheable(value = "symptom-specializations", key = "#symptomCodes.hashCode()")
    public Set<String> findCompatibleSpecializations(List<String> symptomCodes) {
        try {
            return medicalConfigurationMainService.findCompatibleSpecializations(symptomCodes);
        } catch (Exception e) {
            log.error("Error finding compatible specializations for symptoms: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * Get disease-specialization relationships
     */
    @Cacheable(value = "disease-spec-relationships", key = "#diseaseCode")
    public List<String> getDiseaseSpecializationRelationships(String diseaseCode) {
        try {
            return medicalConfigurationMainService.getDiseaseSpecializationRelationships(diseaseCode);
        } catch (Exception e) {
            log.error("Error getting disease-specialization relationships for {}: {}", diseaseCode, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get symptoms for a disease
     */
    @Cacheable(value = "disease-symptoms", key = "#diseaseCode")
    public Set<String> getSymptomsByDisease(String diseaseCode) {
        try {
            return medicalConfigurationMainService.getSymptomsByDisease(diseaseCode);
        } catch (Exception e) {
            log.error("Error getting symptoms for disease {}: {}", diseaseCode, e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * Get diseases for symptoms
     */
    public Set<String> getDiseasesBySymptoms(List<String> symptomCodes) {
        try {
            return medicalConfigurationMainService.getDiseasesBySymptoms(symptomCodes);
        } catch (Exception e) {
            log.error("Error getting diseases for symptoms: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * Get recommended specializations based on case data
     */
    public List<String> getRecommendedSpecializations(CaseAnalysisRequest caseData) {
        try {
            return medicalConfigurationMainService.getRecommendedSpecializations(caseData);
        } catch (Exception e) {
            log.error("Error getting recommended specializations: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Check if specialization is compatible with disease
     */
    @Cacheable(value = "spec-disease-compatibility", key = "#specialization + '_' + #diseaseCode")
    public boolean isSpecializationCompatibleWithDisease(String specialization, String diseaseCode) {
        try {
            Boolean result = medicalConfigurationMainService.isSpecializationCompatibleWithDisease(specialization, diseaseCode);
            return result != null ? result : false;
        } catch (Exception e) {
            log.error("Error checking specialization compatibility: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get disease complexity level
     */
    @Cacheable(value = "disease-complexity", key = "#diseaseCode")
    public String getDiseaseComplexity(String diseaseCode) {
        try {
            return medicalConfigurationMainService.getDiseaseComplexity(diseaseCode);
        } catch (Exception e) {
            log.error("Error getting disease complexity for {}: {}", diseaseCode, e.getMessage());
            return "MEDIUM"; // Default complexity
        }
    }

    @CacheEvict(value = {"medical-configs", "diseases", "medications", "symptoms",
            "disease-specializations", "specialization-diseases",
            "symptom-specializations", "disease-spec-relationships",
            "disease-symptoms", "spec-disease-compatibility",
            "disease-complexity"}, allEntries = true)
    public void clearCache() {
        log.info("Medical configuration cache cleared");
    }
}