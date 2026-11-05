package com.example.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto de entrada de la API REST. Las anotaciones de Spring viven solo en este módulo (ADR 0006). */
@SpringBootApplication
@org.springframework.boot.context.properties.ConfigurationPropertiesScan
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
