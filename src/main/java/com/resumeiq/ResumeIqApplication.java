package com.resumeiq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching   // Redis caching for AI responses
@EnableAsync     // Async processing for long AI calls
public class ResumeIqApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResumeIqApplication.class, args);
    }
}