package com.example.api.itinerario;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.example.api.PruebaApi;
import com.example.core.itinerario.CasosDeUsoItinerario;
import com.example.core.itinerario.Checkpoint;
import com.example.core.itinerario.Itinerario;
import com.example.core.ruta.CasosDeUsoRutas;
import com.example.core.ruta.DatosRuta;

class ItinerarioApiTest extends PruebaApi {

    @Autowired
    CasosDeUsoRutas rutas;
    @Autowired
    CasosDeUsoItinerario itinerarios;

    private int crearRuta(String nombre) {
        return rutas.alta(new DatosRuta(nombre, 3000, "Rocoso", "Baja", "Baja")).id();
    }

    private static String cp(String nombre, String hora) {
        return """
                {"nombre":"%s","hora":"%s","latitud":-32.5,"longitud":-69.9,"altitud":1500,"descripcion":"d"}
                """.formatted(nombre, hora);
    }

    private static Checkpoint checkpoint(String nombre, String hora) {
        return new Checkpoint(0, nombre, hora, -32.5, -69.9, 1500, "d");
    }

    @Test
    void devuelveLosCheckpointsEnOrdenConHorasHHmm() throws Exception {
        int id = crearRuta("Itinerario lectura");
        itinerarios.guardar(id, Itinerario.de(List.of(checkpoint("Cumbre", "11:30"), checkpoint("Base", "06:05"))));

        mvc.perform(get("/api/rutas/" + id + "/itinerario"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Base"))
                .andExpect(jsonPath("$[0].hora").value("06:05"))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].latitud").value(-32.5))
                .andExpect(jsonPath("$[1].nombre").value("Cumbre"));
    }

    @Test
    void guardaLaSecuenciaCompletaYDevuelveLaRuta() throws Exception {
        int id = crearRuta("Itinerario guarda");

        mvc.perform(put("/api/rutas/" + id + "/itinerario").contentType(MediaType.APPLICATION_JSON)
                .content("[" + cp("Cumbre", "11:30") + "," + cp("Base", "06:00") + "]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.cantidadCheckpoints").value(2));

        mvc.perform(get("/api/rutas/" + id + "/itinerario"))
                .andExpect(jsonPath("$[0].nombre").value("Base"))
                .andExpect(jsonPath("$[1].nombre").value("Cumbre"));
    }

    @Test
    void reordenarConHorasIgualesConservaElOrdenEnviado() throws Exception {
        int id = crearRuta("Itinerario reorden");
        String original = "[" + cp("A", "08:00") + "," + cp("B", "08:00") + "," + cp("C", "09:00") + "]";
        String invertido = "[" + cp("B", "08:00") + "," + cp("A", "08:00") + "," + cp("C", "09:00") + "]";

        mvc.perform(put("/api/rutas/" + id + "/itinerario").contentType(MediaType.APPLICATION_JSON).content(original))
                .andExpect(status().isOk());
        mvc.perform(get("/api/rutas/" + id + "/itinerario"))
                .andExpect(jsonPath("$[0].nombre").value("A"))
                .andExpect(jsonPath("$[1].nombre").value("B"));

        mvc.perform(put("/api/rutas/" + id + "/itinerario").contentType(MediaType.APPLICATION_JSON).content(invertido))
                .andExpect(status().isOk());
        mvc.perform(get("/api/rutas/" + id + "/itinerario"))
                .andExpect(jsonPath("$[0].nombre").value("B"))
                .andExpect(jsonPath("$[1].nombre").value("A"))
                .andExpect(jsonPath("$[2].nombre").value("C"));
    }

    @Test
    void laRutaBorradorDejaDeSerloAlGuardarDosCheckpoints() throws Exception {
        int id = crearRuta("Itinerario borrador");
        mvc.perform(get("/api/rutas/" + id)).andExpect(jsonPath("$.borrador").value(true));

        mvc.perform(put("/api/rutas/" + id + "/itinerario").contentType(MediaType.APPLICATION_JSON)
                .content("[" + cp("Base", "06:00") + "," + cp("Cumbre", "11:00") + "]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.borrador").value(false));

        mvc.perform(get("/api/rutas/" + id)).andExpect(jsonPath("$.borrador").value(false));
    }

    @Test
    void unCheckpointInvalidoResponde422ConErrores() throws Exception {
        int id = crearRuta("Itinerario invalido");

        mvc.perform(put("/api/rutas/" + id + "/itinerario").contentType(MediaType.APPLICATION_JSON)
                .content("[" + cp("", "25:99") + "," + cp("Cumbre", "11:00") + "]"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.errores[*].campo", hasItem("checkpoints")));
    }

    @Test
    void faltarUnNumeroEsUnErrorDeValidacion() throws Exception {
        int id = crearRuta("Itinerario sin numero");

        mvc.perform(put("/api/rutas/" + id + "/itinerario").contentType(MediaType.APPLICATION_JSON)
                .content("[{\"nombre\":\"A\",\"hora\":\"06:00\"},{\"nombre\":\"B\",\"hora\":\"07:00\"}]"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void unItinerarioDeUnaRutaInexistenteResponde404() throws Exception {
        mvc.perform(get("/api/rutas/999999/itinerario")).andExpect(status().isNotFound());
        mvc.perform(put("/api/rutas/999999/itinerario").contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isNotFound());
    }
}
