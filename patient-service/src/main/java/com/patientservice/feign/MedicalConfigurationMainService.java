package com.patientservice.feign;

import com.commonlibrary.dto.DiseaseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Set;

@FeignClient(name = "config-service")
public interface MedicalConfigurationMainService {

    @GetMapping("/api/configuration/diseases/{icdCode}")
    DiseaseDto getDiseaseByCode(String icdCode);

    @GetMapping("/api/configuration/diseases/custom/{icdCode}")
    DiseaseDto findDiseaseByIcdCodeCustom(String icdCode);

    @GetMapping("/api/configuration/diseases/{diseaseCode}")
    List<String> getSpecializationsForDisease(String diseaseCode);

}
