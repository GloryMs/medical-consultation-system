package com.patientservice.feign;

import com.commonlibrary.dto.CaseAnalysisRequest;
import com.commonlibrary.dto.DiseaseDto;
import com.commonlibrary.dto.DiseaseSearchCriteria;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Set;

@FeignClient(name = "config-service")
public interface MedicalConfigurationMainService {

    @GetMapping("/api/configuration/diseases/{icdCode}")
    DiseaseDto getDiseaseByCode(@PathVariable String icdCode);

    @GetMapping("/api/configuration/diseases/custom/{icdCode}")
    DiseaseDto findDiseaseByIcdCodeCustom(@PathVariable String icdCode);

    // ===== NEW METHODS FOR CASE ASSIGNMENT =====

    /**
     * Get specializations required for a disease
     */
    @GetMapping("/api/configuration/diseases/{diseaseCode}/specializations")
    List<String> getSpecializationsForDisease(@PathVariable String diseaseCode);

    /**
     * Get diseases that can be handled by a specialization
     */
    @GetMapping("/api/configuration/specializations/{specialization}/diseases")
    List<DiseaseDto> getDiseasesBySpecialization(@PathVariable String specialization);

    /**
     * Find compatible specializations for symptoms
     */
    @PostMapping("/api/configuration/symptoms/compatible-specializations")
    Set<String> findCompatibleSpecializations(@RequestBody List<String> symptomCodes);

    /**
     * Get disease-specialization relationships
     */
    @GetMapping("/api/configuration/diseases/{diseaseCode}/relationships/specializations")
    List<String> getDiseaseSpecializationRelationships(@PathVariable String diseaseCode);

    /**
     * Get symptoms for a disease
     */
    @GetMapping("/api/configuration/diseases/{diseaseCode}/symptoms")
    Set<String> getSymptomsByDisease(@PathVariable String diseaseCode);

    /**
     * Get diseases for symptoms
     */
    @PostMapping("/api/configuration/symptoms/diseases")
    Set<String> getDiseasesBySymptoms(@RequestBody List<String> symptomCodes);

    /**
     * Get recommended specializations based on case data
     */
    @PostMapping("/api/configuration/recommendations/specializations")
    List<String> getRecommendedSpecializations(@RequestBody CaseAnalysisRequest caseData);

    /**
     * Check specialization-disease compatibility
     */
    @GetMapping("/api/configuration/specializations/{specialization}/diseases/{diseaseCode}/compatible")
    Boolean isSpecializationCompatibleWithDisease(
            @PathVariable String specialization,
            @PathVariable String diseaseCode
    );

    /**
     * Get disease complexity level
     */
    @GetMapping("/api/configuration/diseases/{diseaseCode}/complexity")
    String getDiseaseComplexity(@PathVariable String diseaseCode);

    /**
     * Search diseases by criteria
     */
    @PostMapping("/api/configuration/diseases/search")
    List<DiseaseDto> searchDiseases(@RequestBody DiseaseSearchCriteria criteria);

}
