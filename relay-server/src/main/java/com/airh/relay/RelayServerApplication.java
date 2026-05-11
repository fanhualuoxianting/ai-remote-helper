package com.airh.relay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class RelayServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(RelayServerApplication.class, args);
    }
}
