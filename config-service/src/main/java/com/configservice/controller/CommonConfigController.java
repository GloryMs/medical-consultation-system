package com.configservice.controller;

import com.commonlibrary.dto.DiseaseDto;
import com.commonlibrary.dto.MedicalConfigurationDto;
import com.commonlibrary.dto.MedicationDto;
import com.commonlibrary.dto.SymptomDto;
import com.configservice.ConfigServiceGeneralService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
