package com.documentanalysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DocumentAnalysisApplication {
    public static void main(String[] args) {
        SpringApplication.run(DocumentAnalysisApplication.class, args);
    }
}