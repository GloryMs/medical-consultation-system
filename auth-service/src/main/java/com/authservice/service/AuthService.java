package com.authservice.service;

import com.authservice.dto.*;
import com.authservice.kafka.AuthEventProducer;
import com.authservice.repository.UserRepository;
import com.authservice.security.JwtService;
import com.authservice.entity.User;
import com.commonlibrary.dto.UserDto;
import com.commonlibrary.dto.UserStasDto;
import com.commonlibrary.entity.UserRole;
import com.commonlibrary.entity.UserStatus;
import com.commonlibrary.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final AuthEventProducer authEventProducer;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .status(UserStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .isDeleted(false)
                .timeZone("UTC")
                .preferredLanguage("EN")
                .build();

        User savedUser = userRepository.save(user);

// 🔥 NEW: Send Kafka event after successful registration
        authEventProducer.sendUserRegistrationEvent(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        // Create role-specific profile based on user role
        createRoleSpecificProfile(savedUser, request);

        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        String token = jwtService.generateToken(userDetails, savedUser.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .role(savedUser.getRole().name())
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE && user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            throw new BusinessException("Account is " + user.getStatus(), HttpStatus.FORBIDDEN);
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails, user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .role(user.getRole().name())
                .userId(user.getId())
                .email(user.getEmail())
                .build();
    }

    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        // Simulate Google OAuth - In production, verify the ID token with Google
        String googleId = "google_" + System.currentTimeMillis();
        String email = "user_" + googleId + "@gmail.com";

        User user = userRepository.findByGoogleOAuthId(googleId)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .passwordHash(passwordEncoder.encode("GoogleAuth" + googleId))
                            .googleOAuthId(googleId)
                            .role(request.getRole())
                            .status(UserStatus.ACTIVE)
                            .emailVerified(true)
                            .build();
                    return userRepository.save(newUser);
                });

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails, user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .role(user.getRole().name())
                .userId(user.getId())
                .email(user.getEmail())
                .build();
    }

    private void createRoleSpecificProfile(User user, RegisterRequest request) {
        // This will be handled by respective services (patient-service, doctor-service)
        // through event-driven architecture or API calls
    }

    public Page<UserDto> getAllUsers(int page, int size, UserRole role, UserStatus status){
        Pageable pageable = PageRequest.of(page, size);

        if (role != null && status != null) {
            return userRepository.findByRoleAndStatus(role, status, pageable)
                    .map(this::convertToUserDto);
        } else if (role != null) {
            return userRepository.findByRole(role, pageable)
                    .map(this::convertToUserDto);
        } else if (status != null) {
            return userRepository.findByStatus(status, pageable)
                    .map(this::convertToUserDto);
        } else {
            return userRepository.findAll(pageable)
                    .map(this::convertToUserDto);
        }
    }

    public UserDto convertToUserDto(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setEmail(user.getEmail());
        userDto.setRole(user.getRole().name());
        userDto.setStatus(user.getStatus().name());
        userDto.setEmailVerified(user.getEmailVerified());
        userDto.setCreatedAt(user.getCreatedAt());
        userDto.setUpdatedAt(user.getUpdatedAt());
        userDto.setLastLoginAt(user.getLastLogin());
        return userDto;
    }

    public UserStasDto getUsersStats(){
        UserStasDto userStasDto = new UserStasDto();
        List<UserRole> allRoles = new ArrayList<>();
        allRoles.add(UserRole.ADMIN);
        allRoles.add(UserRole.PATIENT);
        allRoles.add(UserRole.DOCTOR);
        userStasDto.setTotalUsers(userRepository.countByRoleIn(allRoles));
        userStasDto.setActiveUsers(userRepository.countByStatus(UserStatus.ACTIVE));
        userStasDto.setPatients(userRepository.countByStatus(UserStatus.PENDING_VERIFICATION));
        userStasDto.setDoctors(userRepository.countByRole(UserRole.DOCTOR));
        userStasDto.setPatients(userRepository.countByRole(UserRole.PATIENT));
        userStasDto.setAdmins(userRepository.countByRole(UserRole.ADMIN));
        return userStasDto;
    }
}
