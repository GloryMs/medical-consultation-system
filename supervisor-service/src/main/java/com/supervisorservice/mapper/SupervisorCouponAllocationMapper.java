package com.supervisorservice.mapper;

import com.commonlibrary.dto.coupon.CouponDistributionEvent;
import com.commonlibrary.dto.coupon.SupervisorCouponAllocationDto;
import com.commonlibrary.entity.SupervisorCouponStatus;
import com.supervisorservice.entity.SupervisorCouponAllocation;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between SupervisorCouponAllocation entity and DTOs.
 */
@Component
public class SupervisorCouponAllocationMapper {

    /**
     * Convert entity to DTO
     */
    public SupervisorCouponAllocationDto toDto(SupervisorCouponAllocation entity) {
        if (entity == null) return null;

        return SupervisorCouponAllocationDto.builder()
                .id(entity.getId())
                .adminCouponId(entity.getAdminCouponId())
                .couponCode(entity.getCouponCode())
                .supervisorId(entity.getSupervisorId())
                .assignedPatientId(entity.getAssignedPatientId())
                .assignedAt(entity.getAssignedAt())
                .discountType(entity.getDiscountType())
                .discountValue(entity.getDiscountValue())
                .maxDiscountAmount(entity.getMaxDiscountAmount())
                .currency(entity.getCurrency())
                .status(entity.getStatus())
                .usedAt(entity.getUsedAt())
                .usedForCaseId(entity.getUsedForCaseId())
                .usedForPaymentId(entity.getUsedForPaymentId())
                .expiresAt(entity.getExpiresAt())
                .isExpiringSoon(entity.isExpiringSoon())
                .daysUntilExpiry(entity.getDaysUntilExpiry())
                .receivedAt(entity.getReceivedAt())
                .lastSyncedAt(entity.getLastSyncedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Convert list of entities to DTOs
     */
    public List<SupervisorCouponAllocationDto> toDtoList(List<SupervisorCouponAllocation> entities) {
        if (entities == null) return List.of();
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert CouponDistributionEvent to entity
     */
    public SupervisorCouponAllocation fromDistributionEvent(CouponDistributionEvent event) {
        if (event == null) return null;

        return SupervisorCouponAllocation.builder()
                .adminCouponId(event.getCouponId())
                .couponCode(event.getCouponCode())
                .supervisorId(event.getBeneficiaryId())
                .discountType(event.getDiscountType())
                .discountValue(event.getDiscountValue())
                .maxDiscountAmount(event.getMaxDiscountAmount())
                .currency(event.getCurrency() != null ? event.getCurrency() : "USD")
                .status(SupervisorCouponStatus.AVAILABLE)
                .expiresAt(event.getExpiresAt())
                .receivedAt(LocalDateTime.now())
                .lastSyncedAt(LocalDateTime.now())
                .batchId(event.getBatchId())
                .batchCode(event.getBatchCode())
                .build();
    }

    /**
     * Enrich DTO with patient information
     */
    public SupervisorCouponAllocationDto enrichWithPatientInfo(
            SupervisorCouponAllocationDto dto,
            String patientName) {
        if (dto == null) return null;
        dto.setAssignedPatientName(patientName);
        return dto;
    }
}