package com.example.api.salida;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import com.example.api.PruebaApi;
import com.example.core.cliente.CasosDeUsoClientes;
import com.example.core.cliente.Cliente;
import com.example.core.cliente.DatosCliente;
import com.example.core.inventario.CasosDeUsoInventario;
import com.example.core.inventario.DatosElemento;
import com.example.core.inventario.ElementoInventario;
import com.example.core.inventario.EstadoElemento;
import com.example.core.itinerario.Checkpoint;
import com.example.core.ruta.CasosDeUsoRutas;
import com.example.core.ruta.DatosRuta;
import com.example.core.ruta.Ruta;
import com.example.core.salida.CopiaOperativa;
import com.example.sqlite.RepositorioSalidasSqlite;

/** Lectura de Salidas con el reloj fijado al 2030-06-15 para que la condición de vencida sea determinista. */
@Import(SalidasApiTest.RelojFijo.class)
class SalidasApiTest extends PruebaApi {

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojFijo() {
            return Clock.fixed(Instant.parse("2030-06-15T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    private static final AtomicInteger dni = new AtomicInteger(50_000_000 + ThreadLocalRandom.current().nextInt(9_000_000));

    @Autowired
    RepositorioSalidasSqlite salidas;
    @Autowired
    CasosDeUsoRutas rutas;
    @Autowired
    CasosDeUsoInventario inventario;
    @Autowired
    CasosDeUsoClientes clientes;

    private int sembrar(String nombre, LocalDate fecha) {
        Ruta ruta = rutas.alta(new DatosRuta(nombre, 4200, "Rocoso", "Media", "Alta"));
        ElementoInventario carpa = inventario.alta(new DatosElemento("Carpa " + nombre, "Campamento", 5, EstadoElemento.DISPONIBLE));
        Cliente cliente = clientes.alta(new DatosCliente(String.valueOf(dni.incrementAndGet()), "Ana " + nombre, "Gomez",
                LocalDate.of(1990, 1, 1), null, "3511234567", "Femenino", null, null, null, false, null, null, null, null));
        return salidas.guardar(new CopiaOperativa(fecha, ruta,
                List.of(new Checkpoint(0, "Base " + nombre, "08:00", -33.0, -69.0, 3000, "Inicio")),
                List.of(new CopiaOperativa.LineaEquipamiento(carpa, 2)), List.of(cliente)));
    }

    @Test
    void listaVaciaNoFalla() throws Exception {
        mvc.perform(get("/api/salidas")).andExpect(status().isOk());
    }

    @Test
    void salidaInexistenteResponde404() throws Exception {
        mvc.perform(get("/api/salidas/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/salidas/999999/copia-operativa")).andExpect(status().isNotFound());
    }

    @Test
    void listaConResumenYCondicionDeVencida() throws Exception {
        int vencida = sembrar("Salida Lista vencida", LocalDate.of(2030, 6, 14));
        int futura = sembrar("Salida Lista futura", LocalDate.of(2030, 6, 15));

        mvc.perform(get("/api/salidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + vencida + ")].vencida", hasItem(true)))
                .andExpect(jsonPath("$[?(@.id==" + futura + ")].vencida", hasItem(false)))
                .andExpect(jsonPath("$[?(@.id==" + vencida + ")].rutaNombre", hasItem("Salida Lista vencida")))
                .andExpect(jsonPath("$[?(@.id==" + vencida + ")].participantes", hasItem(containsString("Ana Salida Lista vencida"))))
                .andExpect(jsonPath("$[?(@.id==" + vencida + ")].equipamiento", hasItem(containsString("Carpa Salida Lista vencida"))));
    }

    @Test
    void detalleIncluyeEstado() throws Exception {
        int id = sembrar("Salida Detalle", LocalDate.of(2030, 7, 1));

        mvc.perform(get("/api/salidas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id)))
                .andExpect(jsonPath("$.estado", is("PLANIFICADA")))
                .andExpect(jsonPath("$.fecha", is("2030-07-01")))
                .andExpect(jsonPath("$.rutaNombre", is("Salida Detalle")));
    }

    @Test
    void copiaOperativaTraeRutaCheckpointsEquipamientoYParticipantes() throws Exception {
        int id = sembrar("Salida Copia", LocalDate.of(2030, 7, 2));

        mvc.perform(get("/api/salidas/" + id + "/copia-operativa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha", is("2030-07-02")))
                .andExpect(jsonPath("$.ruta.nombre", is("Salida Copia")))
                .andExpect(jsonPath("$.checkpoints[0].nombre", is("Base Salida Copia")))
                .andExpect(jsonPath("$.equipamiento[0].nombre", is("Carpa Salida Copia")))
                .andExpect(jsonPath("$.equipamiento[0].cantidad", is(2)))
                .andExpect(jsonPath("$.participantes[0].nombre", is("Ana Salida Copia")));
    }
}
