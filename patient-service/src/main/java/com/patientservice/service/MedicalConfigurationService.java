package com.patientservice.service;

import com.commonlibrary.entity.Disease;
import com.commonlibrary.entity.MedicalConfiguration;
import com.commonlibrary.entity.Medication;
import com.commonlibrary.entity.Symptom;
import com.commonlibrary.exception.BusinessException;
import com.commonlibrary.repository.DiseaseRepository;
import com.commonlibrary.repository.MedicalConfigurationRepository;
import com.commonlibrary.repository.MedicationRepository;
import com.commonlibrary.repository.SymptomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalConfigurationService {

    private final MedicalConfigurationRepository configRepository;
    private final DiseaseRepository diseaseRepository;
    private final MedicationRepository medicationRepository;
    private final SymptomRepository symptomRepository;

    @Cacheable(value = "medical-configs", key = "#configType")
    public List<MedicalConfiguration> getConfigurationsByType(String configType) {
        return configRepository.findByConfigTypeAndIsActiveTrueOrderBySortOrder(configType);
    }

    @Cacheable(value = "diseases")
    public List<Disease> getAllActiveDiseases() {
        return diseaseRepository.findByIsActiveTrueOrderByCategory();
    }

    @Cacheable(value = "diseases-by-category", key = "#category")
    public List<Disease> getDiseasesByCategory(String category) {
        return diseaseRepository.findByCategoryAndIsActiveTrueOrderByName(category);
    }

    @Cacheable(value = "medications")
    public List<Medication> getAllActiveMedications() {
        return medicationRepository.findByIsActiveTrueOrderByName();
    }

    @Cacheable(value = "medications-by-category", key = "#category")
    public List<Medication> getMedicationsByCategory(String category) {
        return medicationRepository.findByCategoryAndIsActiveTrueOrderByName(category);
    }

    @Cacheable(value = "symptoms")
    public List<Symptom> getAllActiveSymptoms() {
        return symptomRepository.findByIsActiveTrueOrderByBodySystem();
    }

    @Cacheable(value = "symptoms-by-system", key = "#bodySystem")
    public List<Symptom> getSymptomsByBodySystem(String bodySystem) {
        return symptomRepository.findByBodySystemAndIsActiveTrueOrderByName(bodySystem);
    }

    public Disease getDiseaseByCode(String icdCode) {
        Disease disease = diseaseRepository.findByIcdCodeAndIsActiveTrue(icdCode)
                .orElseThrow(() -> new BusinessException("Disease not found: " + icdCode, HttpStatus.NOT_FOUND));
        return disease;
    }

    public Disease findDiseaseByIcdCodeCustom(String icdCode) {
        Disease disease;
        try{
            disease = diseaseRepository.findDiseaseByIcdCodeCustom(icdCode).orElseThrow();
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException("Disease not found: " + icdCode, HttpStatus.NOT_FOUND);
        }

        return disease;
    }

    public boolean isDiseaseExisted(String icdCode) {
        boolean isExsited = diseaseRepository.existsByIcdCode(icdCode);
        return isExsited;
    }

    public Set<String> getSpecializationsForDisease(String diseaseCode) {
        return diseaseRepository.findByIcdCodeAndIsActiveTrue(diseaseCode)
            .map(Disease::getRequiredSpecializations)
            .orElse(Collections.emptySet());
    }

    public Set<String> getRelatedDiseasesForSymptoms(Set<String> symptomCodes) {
        List<Symptom> symptoms = symptomRepository.findByCodeInAndIsActiveTrue(symptomCodes);
        return symptoms.stream()
            .flatMap(symptom -> symptom.getRelatedDiseases().stream())
            .collect(Collectors.toSet());
    }

    @CacheEvict(value = {"medical-configs", "diseases", "medications", "symptoms"}, allEntries = true)
    public void clearCache() {
        log.info("Medical configuration cache cleared");
    }
}