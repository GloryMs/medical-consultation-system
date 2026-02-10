package com.adminservice.service;

import com.adminservice.feign.SupervisorServiceClient;
import com.adminservice.kafka.AdminEventProducer;
import com.commonlibrary.dto.*;
import com.commonlibrary.entity.SupervisorVerificationStatus;
import com.commonlibrary.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing supervisors from admin-service
 * Delegates to supervisor-service via Feign client
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupervisorManagementService {

    private final SupervisorServiceClient supervisorServiceClient;
    private final AdminEventProducer adminEventProducer;

    /**
     * Get all supervisors with optional status filter
     *
     * @param status Optional verification status filter
     * @return List of supervisor profiles
     */
    public List<SupervisorProfileDto> getAllSupervisors(SupervisorVerificationStatus status) {
        try {
            log.info("Fetching all supervisors with status: {}", status);

            ResponseEntity<ApiResponse<List<SupervisorProfileDto>>> response =
                    supervisorServiceClient.getAllSupervisors(status);

            if (response.getBody() != null && response.getBody().getData() != null) {
                List<SupervisorProfileDto> supervisors = response.getBody().getData();
                log.info("Retrieved {} supervisors", supervisors.size());
                return supervisors;
            } else {
                log.warn("Empty response when fetching supervisors");
                return List.of();
            }

        } catch (Exception e) {
            log.error("Error fetching supervisors: {}", e.getMessage(), e);
            throw new BusinessException(
                    "Failed to fetch supervisors: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Get supervisors pending verification
     *
     * @return List of supervisors pending verification
     */
    public List<SupervisorProfileDto> getPendingSupervisors() {
        try {
            log.info("Fetching pending supervisor verifications");

            ResponseEntity<ApiResponse<List<SupervisorProfileDto>>> response =
                    supervisorServiceClient.getPendingSupervisors();

            if (response.getBody() != null && response.getBody().getData() != null) {
                List<SupervisorProfileDto> supervisors = response.getBody().getData();
                log.info("Retrieved {} pending supervisors", supervisors.size());
                return supervisors;
            } else {
                log.warn("Empty response when fetching pending supervisors");
                return List.of();
            }

        } catch (Exception e) {
            log.error("Error fetching pending supervisors: {}", e.getMessage(), e);
            throw new BusinessException(
                    "Failed to fetch pending supervisors: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Get supervisor by ID
     *
     * @param supervisorId Supervisor ID
     * @return Supervisor profile
     */
    public SupervisorProfileDto getSupervisor(Long supervisorId) {
        try {
            log.info("Fetching supervisor details for ID: {}", supervisorId);

            ResponseEntity<ApiResponse<SupervisorProfileDto>> response =
                    supervisorServiceClient.getSupervisor(supervisorId);

            if (response.getBody() != null && response.getBody().getData() != null) {
                SupervisorProfileDto supervisor = response.getBody().getData();
                log.info("Successfully retrieved supervisor: {}", supervisor.getFullName());
                return supervisor;
            } else {
                log.error("No data returned for supervisor ID: {}", supervisorId);
                throw new BusinessException("Supervisor not found", HttpStatus.NOT_FOUND);
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching supervisor ID {}: {}", supervisorId, e.getMessage(), e);
            throw new BusinessException(
                    "Failed to retrieve supervisor details: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Verify a supervisor
     *
     * @param adminUserId Admin user ID performing the verification
     * @param supervisorId Supervisor ID to verify
     * @param request Verification request with notes
     * @return Updated supervisor profile
     */
    @Transactional
    public SupervisorProfileDto verifySupervisor(Long adminUserId, Long supervisorId, VerifySupervisorRequest request) {
        try {
            log.info("Admin {} verifying supervisor ID: {}", adminUserId, supervisorId);

            ResponseEntity<ApiResponse<SupervisorProfileDto>> response =
                    supervisorServiceClient.verifySupervisor(adminUserId, supervisorId, request);

            if (response.getBody() != null && response.getBody().getData() != null) {
                SupervisorProfileDto supervisor = response.getBody().getData();
                log.info("Supervisor {} verified successfully", supervisorId);

                // Publish verification event
                publishSupervisorVerificationEvent(supervisor, true, request.getVerificationNotes());

                return supervisor;
            } else {
                log.error("Empty response when verifying supervisor ID: {}", supervisorId);
                throw new BusinessException(
                        "Failed to verify supervisor: Empty response from service",
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error verifying supervisor ID {}: {}", supervisorId, e.getMessage(), e);
            throw new BusinessException(
                    "Failed to verify supervisor: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Reject a supervisor
     *
     * @param supervisorId Supervisor ID to reject
     * @param request Rejection request with reason
     * @return Updated supervisor profile
     */
    @Transactional
    public SupervisorProfileDto rejectSupervisor(Long supervisorId, RejectSupervisorRequest request) {
        try {
            log.info("Rejecting supervisor ID: {} with reason: {}", supervisorId, request.getRejectionReason());

            if (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty()) {
                throw new BusinessException(
                        "Rejection reason is required",
                        HttpStatus.BAD_REQUEST
                );
            }

            ResponseEntity<ApiResponse<SupervisorProfileDto>> response =
                    supervisorServiceClient.rejectSupervisor(supervisorId, request);

            if (response.getBody() != null && response.getBody().getData() != null) {
                SupervisorProfileDto supervisor = response.getBody().getData();
                log.info("Supervisor {} rejected successfully", supervisorId);

                // Publish rejection event
                publishSupervisorVerificationEvent(supervisor, false, request.getRejectionReason());

                return supervisor;
            } else {
                throw new BusinessException(
                        "Failed to reject supervisor: Empty response from service",
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error rejecting supervisor ID {}: {}", supervisorId, e.getMessage(), e);
            throw new BusinessException(
                    "Failed to reject supervisor: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Suspend a supervisor
     *
     * @param supervisorId Supervisor ID to suspend
     * @param reason Suspension reason
     * @return Updated supervisor profile
     */
    @Transactional
    public SupervisorProfileDto suspendSupervisor(Long supervisorId, String reason) {
        try {
            log.info("Suspending supervisor ID: {} with reason: {}", supervisorId, reason);

            if (reason == null || reason.trim().isEmpty()) {
                throw new BusinessException(
                        "Suspension reason is required",
                        HttpStatus.BAD_REQUEST
                );
            }

            ResponseEntity<ApiResponse<SupervisorProfileDto>> response =
                    supervisorServiceClient.suspendSupervisor(supervisorId, reason);

            if (response.getBody() != null && response.getBody().getData() != null) {
                SupervisorProfileDto supervisor = response.getBody().getData();
                log.info("Supervisor {} suspended successfully", supervisorId);

                // Publish suspension event
                publishSupervisorSuspensionEvent(supervisor, reason);

                return supervisor;
            } else {
                throw new BusinessException(
                        "Failed to suspend supervisor: Empty response from service",
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error suspending supervisor ID {}: {}", supervisorId, e.getMessage(), e);
            throw new BusinessException(
                    "Failed to suspend supervisor: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Update supervisor limits
     *
     * @param supervisorId Supervisor ID
     * @param request Limits update request
     * @return Updated supervisor profile
     */
    @Transactional
    public SupervisorProfileDto updateLimits(Long supervisorId, UpdateSupervisorLimitsRequest request) {
        try {
            log.info("Updating limits for supervisor ID {}: maxPatients={}, maxCases={}",
                    supervisorId, request.getMaxPatientsLimit(), request.getMaxActiveCasesPerPatient());

            ResponseEntity<ApiResponse<SupervisorProfileDto>> response =
                    supervisorServiceClient.updateLimits(supervisorId, request);

            if (response.getBody() != null && response.getBody().getData() != null) {
                SupervisorProfileDto supervisor = response.getBody().getData();
                log.info("Supervisor {} limits updated successfully", supervisorId);
                return supervisor;
            } else {
                throw new BusinessException(
                        "Failed to update supervisor limits: Empty response from service",
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating supervisor ID {} limits: {}", supervisorId, e.getMessage(), e);
            throw new BusinessException(
                    "Failed to update supervisor limits: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Search supervisors
     *
     * @param query Search query
     * @return List of matching supervisors
     */
    public List<SupervisorProfileDto> searchSupervisors(String query) {
        try {
            log.info("Searching supervisors with query: {}", query);

            if (query == null || query.trim().isEmpty()) {
                throw new BusinessException(
                        "Search query is required",
                        HttpStatus.BAD_REQUEST
                );
            }

            ResponseEntity<ApiResponse<List<SupervisorProfileDto>>> response =
                    supervisorServiceClient.searchSupervisors(query);

            if (response.getBody() != null && response.getBody().getData() != null) {
                List<SupervisorProfileDto> supervisors = response.getBody().getData();
                log.info("Found {} supervisors matching query: {}", supervisors.size(), query);
                return supervisors;
            } else {
                log.warn("Empty response when searching supervisors");
                return List.of();
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error searching supervisors: {}", e.getMessage(), e);
            throw new BusinessException(
                    "Failed to search supervisors: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Get supervisor statistics
     *
     * @return Comprehensive supervisor system statistics
     */
    public SupervisorStatisticsDto getStatistics() {
        try {
            log.info("Fetching supervisor statistics");

            ResponseEntity<ApiResponse<SupervisorStatisticsDto>> response =
                    supervisorServiceClient.getSupervisorStatistics();

            if (response.getBody() != null && response.getBody().getData() != null) {
                SupervisorStatisticsDto statistics = response.getBody().getData();
                log.info("Retrieved supervisor statistics: {} total supervisors, {} active",
                        statistics.getTotalSupervisors(), statistics.getActiveSupervisors());
                return statistics;
            } else {
                log.error("Empty response when fetching supervisor statistics");
                throw new BusinessException(
                        "Failed to retrieve supervisor statistics: Empty response from service",
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching supervisor statistics: {}", e.getMessage(), e);
            throw new BusinessException(
                    "Failed to retrieve supervisor statistics: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Publish supervisor verification event
     */
    private void publishSupervisorVerificationEvent(SupervisorProfileDto supervisor, boolean approved, String notes) {
        try {
            log.info("Publishing supervisor verification event for ID: {} - approved: {}",
                    supervisor.getId(), approved);

            // TODO: Use AdminEventProducer to publish event
            // adminEventProducer.sendSupervisorVerificationEvent(supervisor, approved, notes);

        } catch (Exception e) {
            log.error("Failed to publish supervisor verification event: {}", e.getMessage(), e);
            // Don't throw exception - event publishing failure shouldn't fail the operation
        }
    }

    /**
     * Publish supervisor suspension event
     */
    private void publishSupervisorSuspensionEvent(SupervisorProfileDto supervisor, String reason) {
        try {
            log.info("Publishing supervisor suspension event for ID: {}", supervisor.getId());

            // TODO: Use AdminEventProducer to publish event
            // adminEventProducer.sendSupervisorSuspensionEvent(supervisor, reason);

        } catch (Exception e) {
            log.error("Failed to publish supervisor suspension event: {}", e.getMessage(), e);
            // Don't throw exception - event publishing failure shouldn't fail the operation
        }
    }
}