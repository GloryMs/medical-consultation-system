package com.supervisorservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Supervisor Service
 * Manages medical supervisors who handle patient cases on their behalf
 * 
 * @author Medical Consultation System
 * @version 1.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableKafka
@EnableScheduling
//@EnableJpaAuditing
@ComponentScan(basePackages = {"com.supervisorservice", "com.commonlibrary"})
public class SupervisorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupervisorServiceApplication.class, args);
    }
}
