package com.doctorservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication(scanBasePackages = {"com.doctorservice", "com.commonlibrary"})
@EnableDiscoveryClient
@EnableFeignClients
//@EnableJpaAuditing
@EnableKafka
@EntityScan(basePackages = {"com.commonlibrary.entity", "com.doctorservice.entity"})
public class DoctorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DoctorServiceApplication.class, args);
    }

}
