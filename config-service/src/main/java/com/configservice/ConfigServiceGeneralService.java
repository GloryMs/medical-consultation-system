package com.configservice;

import com.commonlibrary.dto.*;
import com.configservice.entity.Disease;
import com.configservice.entity.MedicalConfiguration;
import com.configservice.entity.Medication;
import com.configservice.entity.Symptom;
import com.configservice.repository.DiseaseRepository;
import com.configservice.repository.MedicalConfigurationRepository;
import com.configservice.repository.MedicationRepository;
import com.configservice.repository.SymptomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigServiceGeneralService {
    private final DiseaseRepository diseaseRepository;
    private final MedicationRepository medicationRepository;
    private final SymptomRepository symptomRepository;
    private final MedicalConfigurationRepository medicalConfigurationRepository;
    private final ModelMapper modelMapper=new ModelMapper();


    public List<DiseaseDto> findByCategoryAndIsActiveTrueOrderByName(String category){
        List<DiseaseDto> diseases = diseaseRepository.findByCategoryAndIsActiveTrueOrderByName(category).
                stream().map(this::convertToDiseasesDto).toList();
        return diseases;
    }

    public List<DiseaseDto> findAllDiseases(){
        List<DiseaseDto> diseases = diseaseRepository.findAll().
                stream().map(this::convertToDiseasesDto).toList();
        return diseases;
    }

    public List<MedicationDto> findAllMedications(){
        List<MedicationDto> medications = medicationRepository.findAll().
                stream().map(this::convertToMedicationDto).toList();

        return medications;
    }

    public List<SymptomDto> findAllSymptoms(){
        List<SymptomDto> symptoms = symptomRepository.findAll().
                stream().map(this::convertToSymptomDto).toList();
        return symptoms;
    }

    public List<SymptomDto> findSymptomsByIsActiveAndBodySystem(String bodySystem){
        List<SymptomDto> symptoms = symptomRepository.findByBodySystemAndIsActiveTrueOrderByName(bodySystem).
                stream().map(this::convertToSymptomDto).toList();
        return symptoms;
    }

    public List<MedicalConfigurationDto> findMedicalConfigurationsByConfigType( String configType){
        List<MedicalConfigurationDto> medicalConfigurations = medicalConfigurationRepository.
                findByConfigTypeAndIsActiveTrueOrderBySortOrder(configType).stream().
                map(this::convertToMedicalConfigurationDto).toList();
        return medicalConfigurations;
    }



    public DiseaseDto convertToDiseasesDto(Disease disease) {
        return modelMapper.map(disease, DiseaseDto.class);
    }

    public MedicationDto convertToMedicationDto(Medication medication) {
        return modelMapper.map(medication, MedicationDto.class);
    }

    public SymptomDto convertToSymptomDto(Symptom symptom) {
        return modelMapper.map(symptom, SymptomDto.class);
    }

    public MedicalConfigurationDto convertToMedicalConfigurationDto(MedicalConfiguration medConfig) {
        return modelMapper.map(medConfig, MedicalConfigurationDto.class);
    }

    // ===== NEW METHODS FOR CASE ASSIGNMENT =====

    /**
     * Get specializations required for a specific disease
     */
    public List<String> getSpecializationsForDisease(String diseaseCode) {
        Optional<Disease> disease = diseaseRepository.findByIcdCodeAndIsActiveTrue(diseaseCode);
        if (disease.isPresent() && disease.get().getRequiredSpecializations() != null) {
            return new ArrayList<>(disease.get().getRequiredSpecializations());
        }
        return new ArrayList<>();
    }

    /**
     * Get diseases that can be handled by a specific specialization
     */
    public List<DiseaseDto> getDiseasesBySpecialization(String specialization) {
        List<Disease> diseases = diseaseRepository.findByRequiredSpecializationsContaining(specialization);
        return diseases.stream()
                .map(this::mapDiseaseToDto)
                .collect(Collectors.toList());
    }

    /**
     * Find compatible specializations for given symptoms
     */
    public Set<String> findCompatibleSpecializations(List<String> symptomCodes) {
        Set<String> compatibleSpecs = new HashSet<>();

        try {
            // Find diseases associated with these symptoms
            Set<String> relatedDiseases = getDiseasesBySymptoms(symptomCodes);

            // Get specializations for those diseases
            for (String diseaseCode : relatedDiseases) {
                List<String> diseaseSpecs = getSpecializationsForDisease(diseaseCode);
                compatibleSpecs.addAll(diseaseSpecs);
            }
        } catch (Exception e) {
            log.error("Error finding compatible specializations for symptoms: {}", e.getMessage());
        }

        return compatibleSpecs;
    }

    /**
     * Get disease-specialization relationships
     */
    public List<String> getDiseaseSpecializationRelationships(String diseaseCode) {
        return getSpecializationsForDisease(diseaseCode);
    }

    /**
     * Get symptoms associated with a disease
     */
    public Set<String> getSymptomsByDisease(String diseaseCode) {
        Optional<Disease> disease = diseaseRepository.findByIcdCodeAndIsActiveTrue(diseaseCode);
        if (disease.isPresent() && disease.get().getCommonSymptoms() != null) {
            return new HashSet<>(disease.get().getCommonSymptoms());
        }
        return new HashSet<>();
    }

    /**
     * Get diseases associated with given symptoms
     */
    public Set<String> getDiseasesBySymptoms(List<String> symptomCodes) {
        Set<String> relatedDiseases = new HashSet<>();

        try {
            // Find all diseases that have any of these symptoms
            List<Disease> allDiseases = diseaseRepository.findByIsActiveTrueOrderByCategory();

            for (Disease disease : allDiseases) {
                if (disease.getCommonSymptoms() != null) {
                    // Check if disease has any of the provided symptoms
                    boolean hasMatchingSymptoms = disease.getCommonSymptoms().stream()
                            .anyMatch(symptomCodes::contains);

                    if (hasMatchingSymptoms) {
                        relatedDiseases.add(disease.getIcdCode());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error finding diseases by symptoms: {}", e.getMessage());
        }

        return relatedDiseases;
    }

    /**
     * Get recommended specializations based on case analysis
     */
    public List<String> getRecommendedSpecializations(CaseAnalysisRequest caseData) {
        Set<String> recommendedSpecs = new HashSet<>();

        try {
            // Primary disease specializations
            if (caseData.getPrimaryDiseaseCode() != null) {
                recommendedSpecs.addAll(getSpecializationsForDisease(caseData.getPrimaryDiseaseCode()));
            }

            // Secondary disease specializations
            if (caseData.getSecondaryDiseaseCodes() != null) {
                for (String diseaseCode : caseData.getSecondaryDiseaseCodes()) {
                    recommendedSpecs.addAll(getSpecializationsForDisease(diseaseCode));
                }
            }

            // Symptom-based specializations
            if (caseData.getSymptomCodes() != null && !caseData.getSymptomCodes().isEmpty()) {
                recommendedSpecs.addAll(findCompatibleSpecializations(caseData.getSymptomCodes()));
            }

            // If no specific recommendations, use required specialization
            if (recommendedSpecs.isEmpty() && caseData.getRequiredSpecialization() != null) {
                recommendedSpecs.add(caseData.getRequiredSpecialization());
            }

        } catch (Exception e) {
            log.error("Error getting recommended specializations: {}", e.getMessage());
        }

        return new ArrayList<>(recommendedSpecs);
    }

    /**
     * Check if specialization is compatible with disease
     */
    public boolean isSpecializationCompatibleWithDisease(String specialization, String diseaseCode) {
        try {
            List<String> requiredSpecs = getSpecializationsForDisease(diseaseCode);
            return requiredSpecs.contains(specialization);
        } catch (Exception e) {
            log.error("Error checking specialization compatibility: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get disease complexity level
     */
    public String getDiseaseComplexity(String diseaseCode) {
        try {
            Optional<Disease> disease = diseaseRepository.findByIcdCodeAndIsActiveTrue(diseaseCode);
            if (disease.isPresent()) {
                // Simple logic based on number of required specializations
                Set<String> requiredSpecs = disease.get().getRequiredSpecializations();
                if (requiredSpecs == null || requiredSpecs.isEmpty()) {
                    return "LOW";
                } else if (requiredSpecs.size() == 1) {
                    return "MEDIUM";
                } else if (requiredSpecs.size() >= 2) {
                    return "HIGH";
                } else if (requiredSpecs.size() >= 3) {
                    return "HIGHLY_COMPLEX";
                }
            }
        } catch (Exception e) {
            log.error("Error getting disease complexity: {}", e.getMessage());
        }
        return "MEDIUM"; // Default complexity
    }

    /**
     * Search diseases by multiple criteria
     */
    public List<DiseaseDto> searchDiseases(DiseaseSearchCriteria criteria) {
        try {
            List<Disease> diseases = diseaseRepository.findByIsActiveTrueOrderByCategory();

            return diseases.stream()
                    .filter(disease -> matchesCriteria(disease, criteria))
                    .map(this::mapDiseaseToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error searching diseases: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Check if disease matches search criteria
     */
    private boolean matchesCriteria(Disease disease, DiseaseSearchCriteria criteria) {
        // Category filter
        if (criteria.getCategory() != null && !criteria.getCategory().equals(disease.getCategory())) {
            return false;
        }

        // Specialization filter
        if (criteria.getRequiredSpecialization() != null) {
            if (disease.getRequiredSpecializations() == null ||
                    !disease.getRequiredSpecializations().contains(criteria.getRequiredSpecialization())) {
                return false;
            }
        }

        // Symptom filter
        if (criteria.getSymptoms() != null && !criteria.getSymptoms().isEmpty()) {
            if (disease.getCommonSymptoms() == null) {
                return false;
            }

            boolean hasMatchingSymptom = criteria.getSymptoms().stream()
                    .anyMatch(disease.getCommonSymptoms()::contains);
            if (!hasMatchingSymptom) {
                return false;
            }
        }

        // Severity filter
        if (criteria.getSeverity() != null && !criteria.getSeverity().equals(disease.getDefaultSeverity())) {
            return false;
        }

        return true;
    }

    /**
     * Map Disease entity to DTO
     */
    private DiseaseDto mapDiseaseToDto(Disease disease) {
        DiseaseDto dto = new DiseaseDto();
        dto.setId(disease.getId());
        dto.setIcdCode(disease.getIcdCode());
        dto.setName(disease.getName());
        dto.setDescription(disease.getDescription());
        dto.setCategory(disease.getCategory());
        dto.setSubCategory(disease.getSubCategory());
        dto.setRequiredSpecializations(disease.getRequiredSpecializations());
        dto.setCommonSymptoms(disease.getCommonSymptoms());
        dto.setDefaultSeverity(disease.getDefaultSeverity());
        dto.setIsActive(disease.getIsActive());
        return dto;
    }
}
