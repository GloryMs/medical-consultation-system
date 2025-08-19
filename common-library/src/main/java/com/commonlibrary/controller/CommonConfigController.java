package com.commonlibrary.controller;

import com.commonlibrary.entity.Disease;
import com.commonlibrary.entity.MedicalConfiguration;
import com.commonlibrary.entity.Medication;
import com.commonlibrary.entity.Symptom;
import com.commonlibrary.repository.DiseaseRepository;
import com.commonlibrary.repository.MedicalConfigurationRepository;
import com.commonlibrary.repository.MedicationRepository;
import com.commonlibrary.repository.SymptomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/configuration")
@RequiredArgsConstructor
public class CommonConfigController {

    private final DiseaseRepository diseaseRepository;
    private final MedicationRepository medicationRepository;
    private final SymptomRepository symptomRepository;
    private final MedicalConfigurationRepository medicalConfigurationRepository;

    @GetMapping("/diseases/{category}/active")
    public ResponseEntity<List<Disease>> getAllActiveDiseasesByCategory(
            @PathVariable String category) {
        List<Disease> diseases = diseaseRepository.findByCategoryAndIsActiveTrueOrderByName(category);
        return ResponseEntity.ok(diseases);
    }

    @GetMapping("/diseases")
    public ResponseEntity<List<Disease>> getAllDiseases() {
        List<Disease> diseases = diseaseRepository.findAll();
        return ResponseEntity.ok(diseases);
    }

    @GetMapping("/medications")
    public ResponseEntity<List<Medication>> getAllMedications(){
        List<Medication> medications = medicationRepository.findAll();
        return ResponseEntity.ok(medications);
    }

    @GetMapping("/symptoms")
    public ResponseEntity<List<Symptom>>  getAllActiveSymptoms(){
        List<Symptom> symptoms = symptomRepository.findAll();
        return ResponseEntity.ok(symptoms);
    }

    @GetMapping("/symptoms/system/{bodySystem}")
    public ResponseEntity<List<Symptom>>  getSymptomsByBodySystem(@PathVariable String bodySystem){
        List<Symptom> symptoms = symptomRepository.findByBodySystemAndIsActiveTrueOrderByName(bodySystem);
        return ResponseEntity.ok(symptoms);
    }

    @GetMapping("/{configType}")
    public ResponseEntity<List<MedicalConfiguration>>  getConfigurationsByType(@PathVariable String configType){
        List<MedicalConfiguration> medicalConfigurations = medicalConfigurationRepository.
                findByConfigTypeAndIsActiveTrueOrderBySortOrder(configType);
        return ResponseEntity.ok(medicalConfigurations);
    }
}
