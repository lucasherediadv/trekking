package com.example.api.salida;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.example.api.PruebaApi;
import com.example.core.inventario.CasosDeUsoInventario;
import com.example.core.inventario.DatosElemento;
import com.example.core.inventario.EstadoElemento;
import com.example.core.itinerario.CasosDeUsoItinerario;
import com.example.core.itinerario.Checkpoint;
import com.example.core.itinerario.Itinerario;
import com.example.core.ruta.CasosDeUsoRutas;
import com.example.core.ruta.DatosRuta;

/** Cierre de una Salida: {@code POST /api/salidas/{id}/realizada} y {@code /cancelar}. */
class CerrarSalidaApiTest extends PruebaApi {

    private static final AtomicInteger NOMBRE = new AtomicInteger();

    @Autowired
    CasosDeUsoRutas rutas;
    @Autowired
    CasosDeUsoItinerario itinerarios;
    @Autowired
    CasosDeUsoInventario inventario;

    private static String unico(String base) {
        return base + " " + NOMBRE.incrementAndGet() + " " + System.nanoTime();
    }

    private int ruta() {
        int id = rutas.alta(new DatosRuta(unico("Ruta cierre"), 4200, "Rocoso", "Media", "Alta")).id();
        itinerarios.guardar(id, Itinerario.de(List.of(
                new Checkpoint(0, "Base", "06:00", -33.0, -69.0, 3000, "Inicio"),
                new Checkpoint(0, "Cumbre", "11:00", -33.1, -69.1, 4000, "Fin"))));
        return id;
    }

    private int elemento(int cantidad) {
        return inventario.alta(new DatosElemento(unico("Carpa"), "Campamento", cantidad, EstadoElemento.DISPONIBLE)).id();
    }

    private int salida(String fecha, Integer elementoId, int cantidad) throws Exception {
        String equipamiento = elementoId == null ? "" : "{\"elementoId\":%d,\"cantidad\":%d}".formatted(elementoId, cantidad);
        String respuesta = mvc.perform(post("/api/salidas").contentType(MediaType.APPLICATION_JSON).content("""
                {"fecha":"%s","rutaId":%d,"equipamiento":[%s],"elementosNuevos":[],
                "clientesExistentes":[],"clientesNuevos":[]}
                """.formatted(fecha, ruta(), equipamiento)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return Integer.parseInt(respuesta.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    @Test
    void marcaRealizadaUnaSalidaPlanificadaYSeReflejaEnDetalleYListado() throws Exception {
        int id = salida("2031-03-01", null, 0);

        mvc.perform(post("/api/salidas/" + id + "/realizada")).andExpect(status().isNoContent());

        mvc.perform(get("/api/salidas/" + id)).andExpect(jsonPath("$.estado").value("REALIZADA"));
        mvc.perform(get("/api/salidas"))
                .andExpect(jsonPath("$[?(@.id==%d)].estado".formatted(id)).value(hasItem("REALIZADA")));
    }

    @Test
    void cancelaUnaSalidaPlanificadaYLiberaSuReservaDeStock() throws Exception {
        int carpa = elemento(5);
        int id = salida("2031-03-01", carpa, 5);
        mvc.perform(get("/api/inventario/" + carpa)).andExpect(jsonPath("$.cantidadReservada").value(5));

        mvc.perform(post("/api/salidas/" + id + "/cancelar")).andExpect(status().isNoContent());

        mvc.perform(get("/api/salidas/" + id)).andExpect(jsonPath("$.estado").value("CANCELADA"));
        mvc.perform(get("/api/inventario/" + carpa)).andExpect(jsonPath("$.cantidadReservada").value(0));
        salida("2031-03-02", carpa, 5);
    }

    @Test
    void cerrarUnaSalidaYaCerradaResponde409() throws Exception {
        int realizada = salida("2031-03-01", null, 0);
        int cancelada = salida("2031-03-01", null, 0);
        mvc.perform(post("/api/salidas/" + realizada + "/realizada")).andExpect(status().isNoContent());
        mvc.perform(post("/api/salidas/" + cancelada + "/cancelar")).andExpect(status().isNoContent());

        for (int id : new int[] {realizada, cancelada}) {
            mvc.perform(post("/api/salidas/" + id + "/realizada")).andExpect(status().isConflict());
            mvc.perform(post("/api/salidas/" + id + "/cancelar")).andExpect(status().isConflict());
        }
        mvc.perform(get("/api/salidas/" + realizada)).andExpect(jsonPath("$.estado").value("REALIZADA"));
        mvc.perform(get("/api/salidas/" + cancelada)).andExpect(jsonPath("$.estado").value("CANCELADA"));
    }

    @Test
    void salidaInexistenteResponde404() throws Exception {
        mvc.perform(post("/api/salidas/999999/realizada")).andExpect(status().isNotFound());
        mvc.perform(post("/api/salidas/999999/cancelar")).andExpect(status().isNotFound());
    }

    @Test
    void unaSalidaVencidaDejaDeSerloAlCerrarse() throws Exception {
        int id = salida(LocalDate.now().minusDays(3).toString(), null, 0);
        mvc.perform(get("/api/salidas"))
                .andExpect(jsonPath("$[?(@.id==%d)].vencida".formatted(id)).value(hasItem(true)));

        mvc.perform(post("/api/salidas/" + id + "/realizada")).andExpect(status().isNoContent());

        mvc.perform(get("/api/salidas"))
                .andExpect(jsonPath("$[?(@.id==%d)].vencida".formatted(id)).value(hasItem(false)));
    }
}
