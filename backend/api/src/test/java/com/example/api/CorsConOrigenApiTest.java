package com.example.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "trekking.api.origen-web=http://localhost:5173")
class CorsConOrigenApiTest extends PruebaApi {

    @Test
    void permiteUnicamenteElOrigenConfigurado() throws Exception {
        mvc.perform(get("/api/rutas").header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(get("/api/rutas").header("Origin", "http://otro.example"))
                .andExpect(status().isForbidden());
    }

    @Test
    void permiteDeleteDesdeElOrigenConfigurado() throws Exception {
        mvc.perform(options("/api/clientes/1/ficha-medica")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "DELETE"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
