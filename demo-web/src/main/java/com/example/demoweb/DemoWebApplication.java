package com.example.demoweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.example.demoweb", "com.example.demoservice", "com.example.demorepository"})
@EnableJpaRepositories(basePackages = "com.example.demorepository.repository")
@EntityScan(basePackages = "com.example.demorepository.entity")
public class DemoWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoWebApplication.class, args);
    }

}
