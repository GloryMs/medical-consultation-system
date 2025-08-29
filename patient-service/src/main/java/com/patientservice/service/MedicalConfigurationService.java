package com.patientservice.service;

import com.commonlibrary.dto.DiseaseDto;
import com.commonlibrary.exception.BusinessException;
import com.patientservice.feign.MedicalConfigurationMainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

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
    

    @CacheEvict(value = {"medical-configs", "diseases", "medications", "symptoms"}, allEntries = true)
    public void clearCache() {
        log.info("Medical configuration cache cleared");
    }
}