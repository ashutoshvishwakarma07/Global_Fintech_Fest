package com.gff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application entry point for Global Fintech Fest (GFF) Backend Service.
 */
@SpringBootApplication
@EnableScheduling
public class GffApplication {

    public static void main(String[] args) {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
        SpringApplication.run(GffApplication.class, args);
    }
}
