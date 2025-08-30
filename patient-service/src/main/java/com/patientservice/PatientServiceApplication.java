package com.patientservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication(scanBasePackages = {"com.patientservice", "com.commonlibrary"})
@EnableDiscoveryClient
@EnableFeignClients
//@EnableJpaAuditing
@EnableKafka
@EntityScan(basePackages = {"com.commonlibrary.entity", "com.patientservice.entity"})
public class PatientServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PatientServiceApplication.class, args);
    }

}
