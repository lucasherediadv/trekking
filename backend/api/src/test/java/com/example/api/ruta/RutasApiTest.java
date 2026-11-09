package com.example.api.ruta;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.api.PruebaApi;
import com.example.core.ruta.CasosDeUsoRutas;
import com.example.core.ruta.DatosRuta;

class RutasApiTest extends PruebaApi {

    @Autowired
    CasosDeUsoRutas rutas;

    private int crear(String nombre, boolean activa) {
        int id = rutas.alta(new DatosRuta(nombre, 4200, "Rocoso", "Media", "Alta")).id();
        if (!activa) rutas.darDeBaja(id);
        return id;
    }

    @Test
    void listaSoloLasRutasActivasPorDefecto() throws Exception {
        crear("Lista activa", true);
        crear("Lista de baja", false);

        mvc.perform(get("/api/rutas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", hasItem("Lista activa")))
                .andExpect(jsonPath("$[*].nombre", not(hasItem("Lista de baja"))));
    }

    @Test
    void filtraPorEstado() throws Exception {
        crear("Filtro activa", true);
        crear("Filtro de baja", false);

        mvc.perform(get("/api/rutas").param("estado", "activas"))
                .andExpect(jsonPath("$[*].nombre", hasItem("Filtro activa")))
                .andExpect(jsonPath("$[*].nombre", not(hasItem("Filtro de baja"))));
        mvc.perform(get("/api/rutas").param("estado", "bajas"))
                .andExpect(jsonPath("$[*].nombre", hasItem("Filtro de baja")))
                .andExpect(jsonPath("$[*].nombre", not(hasItem("Filtro activa"))));
        mvc.perform(get("/api/rutas").param("estado", "todas"))
                .andExpect(jsonPath("$[*].nombre", hasItem("Filtro activa")))
                .andExpect(jsonPath("$[*].nombre", hasItem("Filtro de baja")));
    }

    @Test
    void estadoDesconocidoEsUnaPeticionInvalida() throws Exception {
        mvc.perform(get("/api/rutas").param("estado", "otras"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PETICION_INVALIDA"));
    }

    @Test
    void devuelveElDetalleDeUnaRuta() throws Exception {
        int id = crear("Detalle", true);

        mvc.perform(get("/api/rutas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nombre").value("Detalle"))
                .andExpect(jsonPath("$.altitudMaxima").value(4200.0))
                .andExpect(jsonPath("$.tipoTerreno").value("Rocoso"))
                .andExpect(jsonPath("$.dificultadTecnica").value("Media"))
                .andExpect(jsonPath("$.dificultadFisica").value("Alta"))
                .andExpect(jsonPath("$.activa").value(true))
                .andExpect(jsonPath("$.cantidadCheckpoints").value(0))
                .andExpect(jsonPath("$.borrador").value(true));
    }

    @Test
    void unaRutaInexistenteResponde404ConCuerpoUniforme() throws Exception {
        mvc.perform(get("/api/rutas/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensaje").value("Ruta con id 999999 no existe."));
    }
}
