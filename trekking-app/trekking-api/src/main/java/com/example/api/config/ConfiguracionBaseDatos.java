package com.example.api.config;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.sqlite.BaseDatosSqlite;

/** Resuelve la base SQLite igual que {@code ConexionDB} de desktop e inicializa el esquema al arrancar. */
@Configuration
public class ConfiguracionBaseDatos {

    /**
     * Usa la propiedad {@code trekking.db}; si falta, busca hacia arriba la raíz del repositorio
     * (la carpeta que contiene {@code trekking-app/pom.xml}).
     */
    @Bean
    public BaseDatosSqlite baseDatos(@Value("${trekking.db:}") String configurada) {
        BaseDatosSqlite base = new BaseDatosSqlite(rutaArchivoBase(configurada));
        base.inicializarEsquema();
        return base;
    }

    static Path rutaArchivoBase(String configurada) {
        if (configurada != null && !configurada.isBlank()) return Path.of(configurada);

        for (Path directorio = Path.of("").toAbsolutePath().normalize(); directorio != null; directorio = directorio.getParent()) {
            if (Files.isRegularFile(directorio.resolve("trekking-app/pom.xml"))) {
                return directorio.resolve("trekking.db");
            }
        }
        return Path.of("trekking.db");
    }
}
