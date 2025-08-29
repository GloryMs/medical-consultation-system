package com.adminservice.feign;

import com.commonlibrary.dto.DiseaseDto;
import com.commonlibrary.dto.MedicalConfigurationDto;
import com.commonlibrary.dto.MedicationDto;
import com.commonlibrary.dto.SymptomDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


import java.util.List;

@FeignClient(name = "config-service")
public interface CommonConfigClient {

    @GetMapping("/api/configuration/diseases")
    List<DiseaseDto> getAllDiseases();

    @GetMapping("/api/configuration/diseases/{category}/active")
    List<DiseaseDto> getAllActiveDiseasesByCategory(@PathVariable String category);

    @GetMapping("/api/configuration/medications")
    List<MedicationDto> getAllMedications();

    @GetMapping("/api/configuration/symptoms")
    List<SymptomDto> getAllActiveSymptoms();

    @GetMapping("/api/configuration/symptoms/system/{bodySystem}")
    List<SymptomDto> getSymptomsByBodySystem(@PathVariable String bodySystem);

    @GetMapping("/api/configuration/{configType}")
    List<MedicalConfigurationDto>  getConfigurationsByType(@PathVariable String configType);

}
