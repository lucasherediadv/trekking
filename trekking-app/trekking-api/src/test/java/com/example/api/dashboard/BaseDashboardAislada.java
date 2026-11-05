package com.example.api.dashboard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Los indicadores del Dashboard son globales, así que estas pruebas no pueden compartir la base de
 * {@code PruebaApi}: cada subclase declara su propio {@code @DynamicPropertySource} con
 * {@link #nuevaBase()}.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class BaseDashboardAislada {

    static String nuevaBase() {
        try {
            Path directorio = Files.createTempDirectory("trekking-api-dashboard");
            directorio.toFile().deleteOnExit();
            Path base = directorio.resolve("trekking.db");
            base.toFile().deleteOnExit();
            return base.toString();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
