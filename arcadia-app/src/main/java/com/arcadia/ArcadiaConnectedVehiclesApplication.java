package com.arcadia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.arcadia")
@EntityScan(basePackages = "com.arcadia")
@EnableJpaRepositories(basePackages = "com.arcadia")
public class ArcadiaConnectedVehiclesApplication {
    public static void main(String[] args) {
        SpringApplication.run(ArcadiaConnectedVehiclesApplication.class, args);
    }
}