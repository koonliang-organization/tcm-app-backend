package com.tcm.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class TcmAppBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TcmAppBackendApplication.class, args);
    }
}
