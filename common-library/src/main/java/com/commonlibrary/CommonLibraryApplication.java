package com.commonlibrary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class CommonLibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommonLibraryApplication.class, args);
    }

}
