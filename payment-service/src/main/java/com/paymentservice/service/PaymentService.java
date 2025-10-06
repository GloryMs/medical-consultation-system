package com.paymentservice.service;

import com.commonlibrary.dto.PaymentDto;
import com.commonlibrary.entity.PaymentStatus;
import com.commonlibrary.exception.BusinessException;
import com.paymentservice.dto.PaymentHistoryDto;
import com.paymentservice.dto.PaymentReceiptDto;
import com.commonlibrary.dto.ProcessPaymentDto;
import com.paymentservice.entity.Payment;
import com.commonlibrary.entity.PaymentMethod;
import com.paymentservice.kafka.PaymentEventProducer;
import com.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer paymentEventProducer;

    @Transactional
    public PaymentDto processPayment(ProcessPaymentDto dto) {
        Payment payment = Payment.builder()
                .patientId(dto.getPatientId())
                .doctorId(dto.getDoctorId())
                .caseId(dto.getCaseId())
                .paymentType(dto.getPaymentType())
                .amount(dto.getAmount())
                .paymentMethod(PaymentMethod.valueOf(dto.getPaymentMethod().toUpperCase()))
                .status(PaymentStatus.PENDING)
                .currency("USD")
                .build();

        // Calculate platform fee (10%)
        BigDecimal platformFee = dto.getAmount().multiply(new BigDecimal("0.10"));
        payment.setPlatformFee(platformFee);
        payment.setDoctorAmount(dto.getAmount().subtract(platformFee));

        // Simulate payment processing
        simulatePaymentProcessing(payment);

        Payment saved = paymentRepository.save(payment);

        // Send Kafka event
        paymentEventProducer.sendPaymentCompletedEvent(
                dto.getPatientId(),
                dto.getDoctorId(),
                dto.getCaseId(),
                dto.getPaymentType().toString(),
                dto.getAmount().doubleValue(),
                saved.getTransactionId()
        );

        ModelMapper modelMapper = new ModelMapper();

        return modelMapper.map(saved, PaymentDto.class);
    }

    private void simulatePaymentProcessing(Payment payment) {
        // Simulate payment gateway interaction
        payment.setTransactionId("TXN_" + System.currentTimeMillis());
        payment.setGatewayResponse("Payment successful");
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setProcessedAt(LocalDateTime.now());
    }

    // 39. Get Payment History Implementation
    public List<PaymentHistoryDto> getPaymentHistory(Long userId) {
        // Get patient ID from user ID (in real implementation, this would be through a service call)
        List<Payment> payments = paymentRepository.findByPatientIdOrderByCreatedAtDesc(userId);

        return payments.stream()
                .map(this::mapToHistoryDto)
                .collect(Collectors.toList());
    }

    // 40. Get Payment Receipt Implementation
    public PaymentReceiptDto getPaymentReceipt(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException("Payment not found", HttpStatus.NOT_FOUND));

        // Verify user has access to this payment
        if (!payment.getPatientId().equals(userId)) {
            throw new BusinessException("Unauthorized access to payment", HttpStatus.FORBIDDEN);
        }

        PaymentReceiptDto receipt = new PaymentReceiptDto();
        receipt.setReceiptNumber("RCP-" + payment.getId() + "-" + payment.getCreatedAt().toLocalDate());
        receipt.setPaymentId(payment.getId());
        receipt.setTransactionId(payment.getTransactionId());
        receipt.setPaymentDate(payment.getProcessedAt());
        receipt.setPaymentType(payment.getPaymentType().toString());
        receipt.setAmount(payment.getAmount());
        receipt.setPlatformFee(payment.getPlatformFee());
        receipt.setTotalAmount(payment.getAmount());
        receipt.setPaymentMethod( payment.getPaymentMethod().name());
        receipt.setStatus(payment.getStatus().toString());
        receipt.setCurrency(payment.getCurrency());

        // Add payer and payee details
        receipt.setPayerName("Patient ID: " + payment.getPatientId()); // In real app, fetch patient name
        if (payment.getDoctorId() != null) {
            receipt.setPayeeName("Doctor ID: " + payment.getDoctorId()); // In real app, fetch doctor name
        }

        return receipt;
    }

    public List<PaymentHistoryDto> getPatientPaymentHistory(Long patientId) {
        List<Payment> payments = paymentRepository.findByPatientId(patientId);
        return payments.stream()
                .map(this::mapToHistoryDto)
                .collect(Collectors.toList());
    }

    public List<PaymentHistoryDto> getDoctorPaymentHistory(Long doctorId) {
        List<Payment> payments = paymentRepository.findByDoctorId(doctorId);
        return payments.stream()
                .map(this::mapToHistoryDto)
                .collect(Collectors.toList());
    }

    public List<Payment> getAllPaymentsBetweenDates(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        return paymentRepository.findByProcessedAtBetween(start, end);
    }

    public Double getTotalRevenue() {
        return paymentRepository.calculateTotalRevenue();
    }

    public Double getMonthlyRevenue() {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        return paymentRepository.calculateRevenueBetween(startOfMonth, now);
    }

    public Map<String, Object> getPaymentDataBetweenDates(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> data = new HashMap<>();

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<Payment> payments = paymentRepository.findByProcessedAtBetween(start, end);

        // Calculate various metrics
        double totalRevenue = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                .mapToDouble(p -> p.getAmount().doubleValue())
                .sum();

        double subscriptionRevenue = payments.stream()
                .filter(p -> p.getPaymentType().name().equals("SUBSCRIPTION"))
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                .mapToDouble(p -> p.getAmount().doubleValue())
                .sum();

        double consultationRevenue = payments.stream()
                .filter(p -> p.getPaymentType().name().equals("CONSULTATION"))
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                .mapToDouble(p -> p.getAmount().doubleValue())
                .sum();

        data.put("totalRevenue", totalRevenue);
        data.put("subscriptionRevenue", subscriptionRevenue);
        data.put("consultationRevenue", consultationRevenue);
        data.put("platformFees", totalRevenue * 0.1);
        data.put("doctorPayouts", consultationRevenue * 0.9);
        data.put("refunds", 0.0); // Calculate actual refunds
        data.put("netRevenue", totalRevenue * 0.9);

        return data;
    }

    @Transactional
    public void processRefund(Long paymentId, Double amount, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException("Payment not found", HttpStatus.NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BusinessException("Can only refund completed payments", HttpStatus.BAD_REQUEST);
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());
        payment.setRefundReason(reason);

        paymentRepository.save(payment);

        //log.info("Refund processed for payment {}: ${} - Reason: {}", paymentId, amount, reason);
    }

    private PaymentHistoryDto mapToHistoryDto(Payment payment) {
        PaymentHistoryDto dto = new PaymentHistoryDto();
        dto.setId(payment.getId());
        dto.setPaymentType(payment.getPaymentType().toString());
        dto.setAmount(payment.getAmount());
        dto.setStatus(payment.getStatus().toString());
        dto.setPaymentMethod(payment.getPaymentMethod().name());
        dto.setProcessedAt(payment.getProcessedAt());

        String description = payment.getPaymentType().name().equals("SUBSCRIPTION")
                ? "Subscription Payment"
                : "Consultation Payment - Case #" + payment.getCaseId();
        dto.setDescription(description);

        return dto;
    }
}
