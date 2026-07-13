package com.edunexus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EduNexusApplication {

    public static void main(String[] args) {
        SpringApplication.run(EduNexusApplication.class, args);
    }
}
