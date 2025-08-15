package com.adminservice.repository;

import com.adminservice.entity.UserDetailsNew;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserDetailsNew, Long> {
    Long countByRole(String role);
    Long countByStatus(String status);
    
//    @Query("SELECT COUNT(u) FROM UserDetailsNew u WHERE DATE(u.createdAt) = CURRENT_DATE")
//    Long countUsersCreatedToday();
//
//    @Query("SELECT COUNT(u) FROM UserDetailsNew u WHERE MONTH(u.createdAt) = MONTH(CURRENT_DATE) AND YEAR(u.createdAt) = YEAR(CURRENT_DATE)")
//    Long countUsersCreatedThisMonth();
//
//    @Query("SELECT COUNT(u) FROM UserDetailsNew u WHERE u.status = 'ACTIVE' AND MONTH(u.lastLogin) = MONTH(CURRENT_DATE)")
//    Integer countActiveUsersThisMonth();
    
    Page<UserDetailsNew> findByRole(String role, Pageable pageable);
    Page<UserDetailsNew> findByStatus(String status, Pageable pageable);
    Page<UserDetailsNew> findByRoleAndStatus(String role, String status, Pageable pageable);
}