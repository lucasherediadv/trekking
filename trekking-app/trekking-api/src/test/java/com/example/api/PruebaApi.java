package com.example.api;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base de las pruebas de la API: levanta el ensamblado real (casos de uso reales) con SQLite en un
 * directorio temporal, nunca el {@code trekking.db} del repositorio. El directorio es uno por JVM
 * para que el contexto de Spring se comparta entre clases; las pruebas deben usar datos propios
 * (nombres únicos) y no asumir una base vacía.
 *
 * <p>Para agregar pruebas de un recurso: extender esta clase, inyectar los casos de uso de core que
 * hagan falta para preparar datos y consultar solo por {@link #mvc}.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class PruebaApi {

    private static final Path BASE = BaseTemporal.nueva("trekking-api-test");

    @Autowired
    protected MockMvc mvc;

    @DynamicPropertySource
    static void baseTemporal(DynamicPropertyRegistry registro) {
        registro.add("trekking.db", () -> BASE.toString());
    }
}
