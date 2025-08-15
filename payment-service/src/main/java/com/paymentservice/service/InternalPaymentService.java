package com.paymentservice.service;

import com.paymentservice.entity.Payment;
import com.paymentservice.entity.PaymentStatus;
import com.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InternalPaymentService {
    
    private final PaymentRepository paymentRepository;
    
    public Double getAverageConsultationFee() {
        List<Payment> consultationPayments = paymentRepository.findByPaymentType("CONSULTATION");
        if (consultationPayments.isEmpty()) {
            return 0.0;
        }
        
        return consultationPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                .mapToDouble(p -> p.getAmount().doubleValue())
                .average()
                .orElse(0.0);
    }
    
    public List<Object> getSubscriptionPayments() {
        return paymentRepository.findByPaymentType("SUBSCRIPTION").stream()
                .map(this::mapToSubscriptionPaymentDto)
                .collect(Collectors.toList());
    }
    
    public List<Object> getConsultationPayments() {
        return paymentRepository.findByPaymentType("CONSULTATION").stream()
                .map(this::mapToConsultationPaymentDto)
                .collect(Collectors.toList());
    }
    
    private Object mapToSubscriptionPaymentDto(Payment payment) {
        // Map to DTO
        return payment;
    }
    
    private Object mapToConsultationPaymentDto(Payment payment) {
        // Map to DTO
        return payment;
    }
}