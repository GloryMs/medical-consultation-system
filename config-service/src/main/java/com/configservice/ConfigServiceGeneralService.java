package com.configservice;

import com.commonlibrary.dto.DiseaseDto;
import com.commonlibrary.dto.MedicalConfigurationDto;
import com.commonlibrary.dto.MedicationDto;
import com.commonlibrary.dto.SymptomDto;
import com.configservice.entity.Disease;
import com.configservice.entity.MedicalConfiguration;
import com.configservice.entity.Medication;
import com.configservice.entity.Symptom;
import com.configservice.repository.DiseaseRepository;
import com.configservice.repository.MedicalConfigurationRepository;
import com.configservice.repository.MedicationRepository;
import com.configservice.repository.SymptomRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
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
}
