package com.doctorservice.repository;

import com.doctorservice.entity.Doctor;
import com.commonlibrary.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    boolean existsByLicenseNumber(String licenseNumber);
    //List<Doctor> findByPrimarySpecialization(String specialization);
    List<Doctor> findByVerificationStatus(VerificationStatus verificationStatus);
    Long countByVerificationStatus(VerificationStatus status);
    @Query("SELECT D FROM Doctor D WHERE D.verificationStatus = ?1 AND D.isAvailable = ?2" )
    List<Doctor> findbyVerificationStatusAndIsAvailable(VerificationStatus status, Boolean isAvailable);

}
