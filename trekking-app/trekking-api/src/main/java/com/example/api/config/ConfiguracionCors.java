package com.example.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** CORS con un único origen configurable; sin valor no se permite ningún origen cruzado. */
@Configuration
public class ConfiguracionCors implements WebMvcConfigurer {

    private final String origenWeb;

    public ConfiguracionCors(@Value("${trekking.api.origen-web:}") String origenWeb) {
        this.origenWeb = origenWeb == null ? "" : origenWeb.trim();
    }

    @Override
    public void addCorsMappings(CorsRegistry registro) {
        if (origenWeb.isEmpty()) return;
        registro.addMapping("/api/**")
                .allowedOrigins(origenWeb)
                .allowedMethods("GET", "POST", "PUT", "OPTIONS")
                .exposedHeaders("Content-Disposition");
    }
}
