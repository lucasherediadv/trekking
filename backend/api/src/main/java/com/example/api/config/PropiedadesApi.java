package com.example.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/** Propiedades propias de la API ({@code trekking.api.*}). */
@ConfigurationProperties(prefix = "trekking.api")
public record PropiedadesApi(String origenWeb, DataSize maxFichaMedica) {
}
