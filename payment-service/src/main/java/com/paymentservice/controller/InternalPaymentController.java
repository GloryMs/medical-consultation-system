package com.paymentservice.controller;

import com.paymentservice.service.InternalPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class InternalPaymentController {
    
    private final InternalPaymentService internalPaymentService;
    
    @GetMapping("/consultation-fee/average")
    public Double getAverageConsultationFee() {
        return internalPaymentService.getAverageConsultationFee();
    }
    
    @GetMapping("/subscriptions/all")
    public Object getSubscriptionPayments() {
        return internalPaymentService.getSubscriptionPayments();
    }
    
    @GetMapping("/consultations/all")
    public Object getConsultationPayments() {
        return internalPaymentService.getConsultationPayments();
    }
}