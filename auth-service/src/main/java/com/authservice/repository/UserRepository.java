package com.authservice.repository;

import com.commonlibrary.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

//@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByGoogleOAuthId(String googleOAuthId);
}
