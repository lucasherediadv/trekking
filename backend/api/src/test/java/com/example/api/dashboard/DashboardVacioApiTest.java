package com.example.api.dashboard;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

class DashboardVacioApiTest extends BaseDashboardAislada {

    @Autowired
    MockMvc mvc;

    @DynamicPropertySource
    static void base(DynamicPropertyRegistry registro) {
        String ruta = nuevaBase();
        registro.add("trekking.db", () -> ruta);
    }

    @Test
    void conBaseVaciaLosIndicadoresSonCero() throws Exception {
        for (String url : new String[] { "/api/dashboard/indicadores", "/api/dashboard/indicadores-salidas" }) {
            mvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rutasActivas").value(0))
                    .andExpect(jsonPath("$.clientesActivos").value(0))
                    .andExpect(jsonPath("$.salidasProximas").value(0))
                    .andExpect(jsonPath("$.equipoNoDisponible").value(0));
        }
    }

    @Test
    void conBaseVaciaNoHayProximasSalidas() throws Exception {
        mvc.perform(get("/api/dashboard/proximas-salidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
