package ru.mirea.hrsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HrSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(HrSystemApplication.class, args);
    }
}
