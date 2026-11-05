package com.example.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class ConfiguracionApiTest extends PruebaApi {

    @Test
    void publicaElContratoOpenApi() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/rutas']").exists());
    }

    @Test
    void sinOrigenConfiguradoNoPermiteOrigenesCruzados() throws Exception {
        mvc.perform(options("/api/rutas")
                        .header("Origin", "http://otro.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
