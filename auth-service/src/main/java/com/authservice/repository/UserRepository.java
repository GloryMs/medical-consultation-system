package com.authservice.repository;

import com.authservice.entity.User;
import com.commonlibrary.entity.UserRole;
import com.commonlibrary.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

//@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByGoogleOAuthId(String googleOAuthId);

    Page<User> findByRole(UserRole role, Pageable pageable);
    Page<User> findByStatus(UserStatus status, Pageable pageable);
    Page<User> findByRoleAndStatus(UserRole role, UserStatus status, Pageable pageable);

    Long countByRole(UserRole role);
    Long countByStatus(UserStatus status);
    Long countByRoleIn(List<UserRole> roleList);

    Optional<User> findByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumber(String phoneNumber);

    Long countByRoleAndStatus(UserRole role, UserStatus status);
}
