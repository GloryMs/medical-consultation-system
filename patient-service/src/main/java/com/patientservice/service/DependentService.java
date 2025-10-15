package com.patientservice.service;

import com.commonlibrary.exception.BusinessException;
import com.patientservice.dto.CreateDependentDto;
import com.patientservice.dto.DependentDto;
import com.patientservice.dto.UpdateDependentDto;
import com.patientservice.entity.Dependent;
import com.patientservice.entity.Patient;
import com.patientservice.repository.CaseRepository;
import com.patientservice.repository.DependentRepository;
import com.patientservice.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DependentService {

    private final DependentRepository dependentRepository;
    private final PatientRepository patientRepository;
    private final CaseRepository caseRepository;

    /**
     * Create a new dependent for a patient
     */
    @Transactional
    public DependentDto createDependent(Long userId, CreateDependentDto dto) {
        log.info("Creating dependent for user: {}", userId);

        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        // Validate: Check if dependent with same name already exists
        if (dependentRepository.existsByFullNameAndPatientIdAndIsDeletedFalse(dto.getFullName(), patient.getId())) {
            throw new BusinessException("A dependent with this name already exists", HttpStatus.CONFLICT);
        }

        Dependent dependent = Dependent.builder()
                .patient(patient)
                .fullName(dto.getFullName())
                .relationship(dto.getRelationship())
                .dateOfBirth(dto.getDateOfBirth())
                .gender(dto.getGender())
                .medicalHistory(dto.getMedicalHistory())
                .bloodGroup(dto.getBloodGroup())
                .allergies(dto.getAllergies())
                .chronicConditions(dto.getChronicConditions())
                .phoneNumber(dto.getPhoneNumber())
                .isDeleted(false)
                .build();

        dependent = dependentRepository.save(dependent);
        log.info("Dependent created successfully: {}", dependent.getId());

        return convertToDto(dependent);
    }

    /**
     * Get all dependents for a patient
     */
    @Transactional(readOnly = true)
    public List<DependentDto> getDependents(Long userId) {
        log.info("Fetching dependents for user: {}", userId);

        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        List<Dependent> dependents = dependentRepository.findByPatientIdAndIsDeletedFalse(patient.getId());

        return dependents.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific dependent by ID
     */
    @Transactional(readOnly = true)
    public DependentDto getDependent(Long userId, Long dependentId) {
        log.info("Fetching dependent {} for user {}", dependentId, userId);

        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        Dependent dependent = dependentRepository.findByIdAndPatientIdAndIsDeletedFalse(dependentId, patient.getId())
                .orElseThrow(() -> new BusinessException("Dependent not found", HttpStatus.NOT_FOUND));

        return convertToDto(dependent);
    }

    /**
     * Update a dependent
     */
    @Transactional
    public DependentDto updateDependent(Long userId, Long dependentId, UpdateDependentDto dto) {
        log.info("Updating dependent {} for user {}", dependentId, userId);

        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        Dependent dependent = dependentRepository.findByIdAndPatientIdAndIsDeletedFalse(dependentId, patient.getId())
                .orElseThrow(() -> new BusinessException("Dependent not found", HttpStatus.NOT_FOUND));

        // Update fields if provided
        if (dto.getFullName() != null) {
            dependent.setFullName(dto.getFullName());
        }
        if (dto.getRelationship() != null) {
            dependent.setRelationship(dto.getRelationship());
        }
        if (dto.getDateOfBirth() != null) {
            dependent.setDateOfBirth(dto.getDateOfBirth());
        }
        if (dto.getGender() != null) {
            dependent.setGender(dto.getGender());
        }
        if (dto.getMedicalHistory() != null) {
            dependent.setMedicalHistory(dto.getMedicalHistory());
        }
        if (dto.getBloodGroup() != null) {
            dependent.setBloodGroup(dto.getBloodGroup());
        }
        if (dto.getAllergies() != null) {
            dependent.setAllergies(dto.getAllergies());
        }
        if (dto.getChronicConditions() != null) {
            dependent.setChronicConditions(dto.getChronicConditions());
        }
        if (dto.getPhoneNumber() != null) {
            dependent.setPhoneNumber(dto.getPhoneNumber());
        }

        dependent = dependentRepository.save(dependent);
        log.info("Dependent updated successfully: {}", dependent.getId());

        return convertToDto(dependent);
    }

    /**
     * Delete (soft delete) a dependent
     */
    @Transactional
    public void deleteDependent(Long userId, Long dependentId) {
        log.info("Deleting dependent {} for user {}", dependentId, userId);

        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Patient not found", HttpStatus.NOT_FOUND));

        Dependent dependent = dependentRepository.findByIdAndPatientIdAndIsDeletedFalse(dependentId, patient.getId())
                .orElseThrow(() -> new BusinessException("Dependent not found", HttpStatus.NOT_FOUND));

        // Check if dependent has active cases
        long activeCases = caseRepository.countByDependentIdAndStatusNotIn(
                dependentId, 
                List.of(com.commonlibrary.entity.CaseStatus.CLOSED)
        );

        if (activeCases > 0) {
            throw new BusinessException(
                "Cannot delete dependent with active cases. Please close all cases first.", 
                HttpStatus.CONFLICT
            );
        }

        // Soft delete
        dependent.setIsDeleted(true);
        dependentRepository.save(dependent);

        log.info("Dependent deleted successfully: {}", dependent.getId());
    }

    /**
     * Convert Dependent entity to DTO
     */
    private DependentDto convertToDto(Dependent dependent) {
        Integer age = null;
        if (dependent.getDateOfBirth() != null) {
            age = Period.between(dependent.getDateOfBirth(), LocalDate.now()).getYears();
        }

        // Count cases for this dependent
        Long casesCount = caseRepository.countByDependentId(dependent.getId());

        return DependentDto.builder()
                .id(dependent.getId())
                .fullName(dependent.getFullName())
                .relationship(dependent.getRelationship())
                .dateOfBirth(dependent.getDateOfBirth())
                .gender(dependent.getGender())
                .medicalHistory(dependent.getMedicalHistory())
                .bloodGroup(dependent.getBloodGroup())
                .allergies(dependent.getAllergies())
                .chronicConditions(dependent.getChronicConditions())
                .phoneNumber(dependent.getPhoneNumber())
                .profilePicture(dependent.getProfilePicture())
                .age(age)
                .casesCount(casesCount != null ? casesCount.intValue() : 0)
                .build();
    }
}