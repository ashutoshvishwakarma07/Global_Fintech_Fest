package com.gff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application entry point for Global Fintech Fest (GFF) Backend Service.
 */
@SpringBootApplication
public class GffApplication {

    public static void main(String[] args) {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(GffApplication.class, args);
    }
}
