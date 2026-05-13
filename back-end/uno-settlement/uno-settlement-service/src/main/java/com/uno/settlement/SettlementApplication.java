package com.uno.settlement;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.uno")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.uno.order.api")
@MapperScan("com.uno.settlement.mapper")
public class SettlementApplication {
    public static void main(String[] args) {
        SpringApplication.run(SettlementApplication.class, args);
    }
}
