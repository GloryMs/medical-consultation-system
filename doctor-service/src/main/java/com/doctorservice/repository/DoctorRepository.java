package com.doctorservice.repository;

import com.doctorservice.entity.Doctor;
import com.commonlibrary.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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


    // ===== WORKLOAD MANAGEMENT QUERIES =====

    @Query("SELECT d FROM Doctor d WHERE " +
            "d.verificationStatus = 'VERIFIED' AND " +
            "d.isAvailable = true AND " +
            "d.activeCases < d.maxActiveCases AND " +
            "d.todayAppointments < d.maxDailyAppointments")
    List<Doctor> findAvailableDoctorsForAssignment();

    @Query("SELECT d FROM Doctor d WHERE " +
            "d.verificationStatus = 'VERIFIED' AND " +
            "d.isAvailable = true AND " +
            "d.primarySpecialization = :specialization AND " +
            "d.activeCases < d.maxActiveCases AND " +
            "d.todayAppointments < d.maxDailyAppointments " +
            "ORDER BY d.workloadPercentage ASC, d.rating DESC")
    List<Doctor> findAvailableDoctorsBySpecialization(@Param("specialization") String specialization);

    @Query("SELECT d FROM Doctor d WHERE " +
            "d.verificationStatus = 'VERIFIED' AND " +
            "d.workloadPercentage > 80.0 " +
            "ORDER BY d.workloadPercentage DESC")
    List<Doctor> findOverloadedDoctors();

    @Query("SELECT d FROM Doctor d WHERE " +
            "d.verificationStatus = 'VERIFIED' AND " +
            "d.emergencyMode = true")
    List<Doctor> findDoctorsInEmergencyMode();

    @Query("SELECT AVG(d.workloadPercentage) FROM Doctor d WHERE " +
            "d.verificationStatus = 'VERIFIED' AND d.isAvailable = true")
    Double getAverageWorkloadPercentage();

    @Query("SELECT COUNT(d) FROM Doctor d WHERE " +
            "d.verificationStatus = 'VERIFIED' AND " +
            "d.isAvailable = true AND " +
            "d.activeCases < d.maxActiveCases")
    long countAvailableDoctorsForNewCases();

    @Query("SELECT d FROM Doctor d WHERE " +
            "d.verificationStatus = 'VERIFIED' AND " +
            "d.primarySpecialization IN :specializations AND " +
            "d.isAvailable = true AND " +
            "(d.emergencyMode = true OR d.activeCases < d.maxActiveCases) " +
            "ORDER BY d.workloadPercentage ASC, d.rating DESC")
    List<Doctor> findBestMatchDoctors(@Param("specializations") List<String> specializations);

    @Query("SELECT d.primarySpecialization, AVG(d.workloadPercentage) " +
            "FROM Doctor d WHERE d.verificationStatus = 'VERIFIED' " +
            "GROUP BY d.primarySpecialization")
    List<Object[]> getWorkloadBySpecialization();

    @Query("SELECT d FROM Doctor d WHERE " +
            "d.verificationStatus = 'VERIFIED' AND " +
            "d.lastWorkloadUpdate < :cutoffTime")
    List<Doctor> findDoctorsWithOutdatedWorkload(@Param("cutoffTime") java.time.LocalDateTime cutoffTime);

}
