package com.configservice.controller;

import com.commonlibrary.dto.*;
import com.configservice.ConfigServiceGeneralService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/configuration")
@RequiredArgsConstructor
public class CommonConfigController {

    private final ConfigServiceGeneralService configServiceGeneralService;

    @GetMapping("/diseases/{category}/active")
    public ResponseEntity<List<DiseaseDto>> getAllActiveDiseasesByCategory(
            @PathVariable String category) {
        List<DiseaseDto> diseases = configServiceGeneralService.findByCategoryAndIsActiveTrueOrderByName(category);
        return ResponseEntity.ok(diseases);
    }

    @GetMapping("/diseases")
    public ResponseEntity<List<DiseaseDto>> getAllDiseases() {
        List<DiseaseDto> diseases = configServiceGeneralService.findAllDiseases();
        return ResponseEntity.ok(diseases);
    }

    @GetMapping("/medications")
    public ResponseEntity<List<MedicationDto>> getAllMedications(){
        List<MedicationDto> medications = configServiceGeneralService.findAllMedications();
        return ResponseEntity.ok(medications);
    }

    @GetMapping("/symptoms")
    public ResponseEntity<List<SymptomDto>>  getAllActiveSymptoms(){
        List<SymptomDto> symptoms = configServiceGeneralService.findAllSymptoms();
        return ResponseEntity.ok(symptoms);
    }

    @GetMapping("/symptoms/system/{bodySystem}")
    public ResponseEntity<List<SymptomDto>>  getSymptomsByBodySystem(@PathVariable String bodySystem){
        List<SymptomDto> symptoms = configServiceGeneralService.findSymptomsByIsActiveAndBodySystem(bodySystem);
        return ResponseEntity.ok(symptoms);
    }

    @GetMapping("/{configType}")
    public ResponseEntity<List<MedicalConfigurationDto>>  getConfigurationsByType(@PathVariable String configType){
        List<MedicalConfigurationDto> medicalConfigurations = configServiceGeneralService.
                findMedicalConfigurationsByConfigType(configType);
        return ResponseEntity.ok(medicalConfigurations);
    }

    // ===== NEW ENDPOINTS FOR CASE ASSIGNMENT =====

    /**
     * Get specializations required for a specific disease
     */
    @GetMapping("/diseases/{diseaseCode}/specializations")
    public ResponseEntity<List<String>> getSpecializationsForDisease(@PathVariable String diseaseCode) {
        List<String> specializations = configServiceGeneralService.getSpecializationsForDisease(diseaseCode);
        return ResponseEntity.ok(specializations);
    }

    /**
     * Get diseases that match a specific specialization
     */
    @GetMapping("/specializations/{specialization}/diseases")
    public ResponseEntity<List<DiseaseDto>> getDiseasesBySpecialization(@PathVariable String specialization) {
        List<DiseaseDto> diseases = configServiceGeneralService.getDiseasesBySpecialization(specialization);
        return ResponseEntity.ok(diseases);
    }

    @GetMapping("/diseases/{icdCode}")
    public DiseaseDto getDiseaseByCode(@PathVariable String icdCode){
        DiseaseDto disease = configServiceGeneralService.getDiseasesByCode(icdCode);
        return disease;
    }

    /**
     * Find compatible specializations for given symptoms
     */
    @PostMapping("/symptoms/compatible-specializations")
    public ResponseEntity<Set<String>> findCompatibleSpecializations(@RequestBody List<String> symptomCodes) {
        Set<String> compatibleSpecs = configServiceGeneralService.findCompatibleSpecializations(symptomCodes);
        return ResponseEntity.ok(compatibleSpecs);
    }

    /**
     * Get disease-specialization relationships for a disease
     */
    @GetMapping("/diseases/{diseaseCode}/relationships/specializations")
    public ResponseEntity<List<String>> getDiseaseSpecializationRelationships(@PathVariable String diseaseCode) {
        List<String> relationships = configServiceGeneralService.getDiseaseSpecializationRelationships(diseaseCode);
        return ResponseEntity.ok(relationships);
    }

    /**
     * Get symptoms associated with a disease
     */
    @GetMapping("/diseases/{diseaseCode}/symptoms")
    public ResponseEntity<Set<String>> getSymptomsByDisease(@PathVariable String diseaseCode) {
        Set<String> symptoms = configServiceGeneralService.getSymptomsByDisease(diseaseCode);
        return ResponseEntity.ok(symptoms);
    }

    /**
     * Get diseases associated with symptoms
     */
    @PostMapping("/symptoms/diseases")
    public ResponseEntity<Set<String>> getDiseasesBySymptoms(@RequestBody List<String> symptomCodes) {
        Set<String> diseases = configServiceGeneralService.getDiseasesBySymptoms(symptomCodes);
        return ResponseEntity.ok(diseases);
    }

    /**
     * Get recommended specializations based on case data
     */
    @PostMapping("/recommendations/specializations")
    public ResponseEntity<List<String>> getRecommendedSpecializations(@RequestBody CaseAnalysisRequest caseData) {
        List<String> recommendations = configServiceGeneralService.getRecommendedSpecializations(caseData);
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Validate if a specialization can handle a specific disease
     */
    @GetMapping("/specializations/{specialization}/diseases/{diseaseCode}/compatible")
    public ResponseEntity<Boolean> isSpecializationCompatibleWithDisease(
            @PathVariable String specialization,
            @PathVariable String diseaseCode) {
        boolean isCompatible = configServiceGeneralService.isSpecializationCompatibleWithDisease(specialization, diseaseCode);
        return ResponseEntity.ok(isCompatible);
    }

    /**
     * Get disease complexity level
     */
    @GetMapping("/diseases/{diseaseCode}/complexity")
    public ResponseEntity<String> getDiseaseComplexity(@PathVariable String diseaseCode) {
        String complexity = configServiceGeneralService.getDiseaseComplexity(diseaseCode);
        return ResponseEntity.ok(complexity);
    }

    /**
     * Find diseases by multiple criteria for better matching
     */
    @PostMapping("/diseases/search")
    public ResponseEntity<List<DiseaseDto>> searchDiseases(@RequestBody DiseaseSearchCriteria criteria) {
        List<DiseaseDto> diseases = configServiceGeneralService.searchDiseases(criteria);
        return ResponseEntity.ok(diseases);
    }
}
