package com.example.api.ruta;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.example.api.PruebaApi;
import com.example.core.ruta.CasosDeUsoRutas;
import com.example.core.ruta.DatosRuta;

class RutasEscrituraApiTest extends PruebaApi {

    @Autowired
    CasosDeUsoRutas rutas;

    private static String json(String nombre, String altitud, String terreno) {
        return """
                {"nombre":"%s","altitudMaxima":%s,"tipoTerreno":"%s","dificultadTecnica":"Media","dificultadFisica":"Alta"}
                """.formatted(nombre, altitud, terreno);
    }

    private int crear(String nombre) {
        return rutas.alta(new DatosRuta(nombre, 3000, "Rocoso", "Baja", "Baja")).id();
    }

    @Test
    void creaUnaRutaYResponde201ConElRecurso() throws Exception {
        mvc.perform(post("/api/rutas").contentType(MediaType.APPLICATION_JSON).content(json("Escritura crea", "4200", "Rocoso")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Escritura crea"))
                .andExpect(jsonPath("$.altitudMaxima").value(4200.0))
                .andExpect(jsonPath("$.activa").value(true));
    }

    @Test
    void datosInvalidosResponden422ConErroresPorCampo() throws Exception {
        mvc.perform(post("/api/rutas").contentType(MediaType.APPLICATION_JSON).content(json("", "-5", "Marte")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.errores[*].campo", hasItem("nombre")))
                .andExpect(jsonPath("$.errores[*].campo", hasItem("altitudMaxima")))
                .andExpect(jsonPath("$.errores[*].campo", hasItem("tipoTerreno")));
    }

    @Test
    void faltarLaAltitudEsUnErrorDeCampo() throws Exception {
        mvc.perform(post("/api/rutas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Escritura sin altitud\",\"tipoTerreno\":\"Rocoso\","
                        + "\"dificultadTecnica\":\"Media\",\"dificultadFisica\":\"Alta\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("altitudMaxima")));
    }

    @Test
    void editaUnaRuta() throws Exception {
        int id = crear("Escritura edita");

        mvc.perform(put("/api/rutas/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(json("Escritura editada", "5100", "Mixto")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nombre").value("Escritura editada"))
                .andExpect(jsonPath("$.tipoTerreno").value("Mixto"));
        mvc.perform(get("/api/rutas/{id}", id)).andExpect(jsonPath("$.altitudMaxima").value(5100.0));
    }

    @Test
    void editarConDatosInvalidosResponde422() throws Exception {
        int id = crear("Escritura edita mal");

        mvc.perform(put("/api/rutas/{id}", id).contentType(MediaType.APPLICATION_JSON).content(json("", "1", "Rocoso")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("nombre")));
    }

    @Test
    void daDeBajaYReactivaUnaRuta() throws Exception {
        int id = crear("Escritura baja");

        mvc.perform(post("/api/rutas/{id}/baja", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/rutas").param("estado", "bajas"))
                .andExpect(jsonPath("$[*].nombre", hasItem("Escritura baja")));
        mvc.perform(get("/api/rutas").param("estado", "activas"))
                .andExpect(jsonPath("$[*].nombre", not(hasItem("Escritura baja"))));

        mvc.perform(post("/api/rutas/{id}/reactivar", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/rutas").param("estado", "activas"))
                .andExpect(jsonPath("$[*].nombre", hasItem("Escritura baja")));
    }

    @Test
    void operarSobreUnaRutaInexistenteResponde404() throws Exception {
        mvc.perform(put("/api/rutas/{id}", 999999).contentType(MediaType.APPLICATION_JSON)
                .content(json("Nada", "1", "Rocoso"))).andExpect(status().isNotFound());
        mvc.perform(post("/api/rutas/{id}/baja", 999999)).andExpect(status().isNotFound());
        mvc.perform(post("/api/rutas/{id}/reactivar", 999999)).andExpect(status().isNotFound());
    }

    @Test
    void noExisteDelete() throws Exception {
        int id = crear("Escritura sin delete");
        mvc.perform(delete("/api/rutas/{id}", id)).andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/api/rutas/{id}", id)).andExpect(status().isOk());
    }
}
