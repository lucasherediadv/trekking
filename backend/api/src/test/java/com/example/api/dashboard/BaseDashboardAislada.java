package com.example.api.dashboard;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.api.BaseTemporal;

/**
 * Los indicadores del Dashboard son globales, así que estas pruebas no pueden compartir la base de
 * {@code PruebaApi}: cada subclase declara su propio {@code @DynamicPropertySource} con
 * {@link #nuevaBase()}.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class BaseDashboardAislada {

    static String nuevaBase() {
        return BaseTemporal.nueva("trekking-api-dashboard").toString();
    }
}
