package com.adminservice.feign;

import com.commonlibrary.entity.Disease;
import com.commonlibrary.entity.MedicalConfiguration;
import com.commonlibrary.entity.Medication;
import com.commonlibrary.entity.Symptom;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "common-library")
public interface CommonConfigClient {

    @GetMapping("/api/configuration/diseases")
    List<Disease> getAllDiseases();

    @GetMapping("/api/configuration/diseases/{category}/active")
    List<Disease> getAllActiveDiseasesByCategory(@PathVariable String category);

    @GetMapping("/api/configuration/medications")
    List<Medication> getAllMedications();

    @GetMapping("/api/configuration/symptoms")
    List<Symptom> getAllActiveSymptoms();

    @GetMapping("/api/configuration/symptoms/system/{bodySystem}")
    List<Symptom> getSymptomsByBodySystem(@PathVariable String bodySystem);

    @GetMapping("/api/configuration/{configType}")
    List<MedicalConfiguration>  getConfigurationsByType(@PathVariable String configType);

}
