package com.ccems;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class CcemsApplication {
    public static void main(String[] args) {
        SpringApplication.run(CcemsApplication.class, args);
    }
}
