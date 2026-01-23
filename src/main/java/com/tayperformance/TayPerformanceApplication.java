package com.tayperformance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableAsync // Activeert de @Async annotatie voor je SmsService
@EnableTransactionManagement // Optioneel, Spring Boot activeert dit vaak al automatisch
public class TayPerformanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TayPerformanceApplication.class, args);
    }
}
