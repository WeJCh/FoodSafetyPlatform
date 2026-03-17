package com.mortal.regulation.operation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
@MapperScan("com.mortal.regulation.operation.mapper")
public class RegulationOperationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegulationOperationServiceApplication.class, args);
    }
}
