package org.codibly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "org.codibly.externalClient")
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}