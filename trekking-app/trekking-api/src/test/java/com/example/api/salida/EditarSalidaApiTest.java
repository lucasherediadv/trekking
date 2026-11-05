package com.example.api.salida;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.example.api.PruebaApi;
import com.example.core.cliente.CasosDeUsoClientes;
import com.example.core.cliente.DatosCliente;
import com.example.core.inventario.CasosDeUsoInventario;
import com.example.core.inventario.DatosElemento;
import com.example.core.inventario.EstadoElemento;
import com.example.core.itinerario.CasosDeUsoItinerario;
import com.example.core.itinerario.Checkpoint;
import com.example.core.itinerario.Itinerario;
import com.example.core.ruta.CasosDeUsoRutas;
import com.example.core.ruta.DatosRuta;
import com.example.core.salida.CasosDeUsoSalidas;

/** Edición de una Salida planificada: {@code PUT /api/salidas/{id}}. */
class EditarSalidaApiTest extends PruebaApi {

    private static final AtomicInteger DNI = new AtomicInteger(70_000_000 + ThreadLocalRandom.current().nextInt(9_000_000));
    private static final AtomicInteger NOMBRE = new AtomicInteger();

    @Autowired
    CasosDeUsoRutas rutas;
    @Autowired
    CasosDeUsoItinerario itinerarios;
    @Autowired
    CasosDeUsoInventario inventario;
    @Autowired
    CasosDeUsoClientes clientes;
    @Autowired
    CasosDeUsoSalidas salidas;

    private static String unico(String base) {
        return base + " " + NOMBRE.incrementAndGet() + " " + System.nanoTime();
    }

    private int ruta(String nombre) {
        int id = rutas.alta(new DatosRuta(nombre, 4200, "Rocoso", "Media", "Alta")).id();
        itinerarios.guardar(id, Itinerario.de(List.of(
                new Checkpoint(0, "Base " + nombre, "06:00", -33.0, -69.0, 3000, "Inicio"),
                new Checkpoint(0, "Cumbre " + nombre, "11:00", -33.1, -69.1, 4000, "Fin"))));
        return id;
    }

    private int elemento(String nombre, int cantidad) {
        return inventario.alta(new DatosElemento(nombre, "Campamento", cantidad, EstadoElemento.DISPONIBLE)).id();
    }

    private int salida(int ruta, String equipamiento) throws Exception {
        String cuerpo = """
                {"fecha":"2031-03-01","rutaId":%d,"equipamiento":[%s]}
                """.formatted(ruta, equipamiento);
        String respuesta = mvc.perform(post("/api/salidas").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return Integer.parseInt(respuesta.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    private ResultActions editar(int id, String cuerpo) throws Exception {
        return mvc.perform(put("/api/salidas/" + id).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private static String cuerpo(int rutaId, String checkpoints, String equipamiento, String participantes) {
        return """
                {"fecha":"2032-05-10","rutaId":%d,"checkpoints":[%s],"equipamiento":[%s],"participantes":[%s]}
                """.formatted(rutaId, checkpoints, equipamiento, participantes);
    }

    private static String cp(String nombre) {
        return "{\"nombre\":\"%s\",\"hora\":\"07:00\",\"latitud\":-33.0,\"longitud\":-69.0,\"altitud\":3100,\"descripcion\":\"x\"}"
                .formatted(nombre);
    }

    private int cliente(String nombre) {
        return clientes.alta(new DatosCliente(String.valueOf(DNI.incrementAndGet()), nombre, "Gomez",
                LocalDate.of(1990, 1, 1), null, "3511234567", "Femenino", null, null, null, false, null, null, null,
                null)).id();
    }

    @Test
    void editaLaSalidaYReemplazaLaCopiaOperativa() throws Exception {
        int ruta = ruta(unico("Ruta editar"));
        int carpa = elemento(unico("Carpa"), 5);
        int soga = elemento(unico("Soga"), 5);
        int id = salida(ruta, "{\"elementoId\":%d,\"cantidad\":2}".formatted(carpa));
        int participante = cliente("Ana");

        editar(id, cuerpo(ruta, cp("Puesto A") + "," + cp("Puesto B") + "," + cp("Puesto C"),
                "{\"elementoId\":%d,\"cantidad\":3}".formatted(soga), String.valueOf(participante)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.fecha").value("2032-05-10"))
                .andExpect(jsonPath("$.estado").value("PLANIFICADA"));

        mvc.perform(get("/api/salidas/" + id + "/copia-operativa"))
                .andExpect(jsonPath("$.checkpoints.length()").value(3))
                .andExpect(jsonPath("$.checkpoints[0].nombre").value("Puesto A"))
                .andExpect(jsonPath("$.equipamiento.length()").value(1))
                .andExpect(jsonPath("$.equipamiento[0].cantidad").value(3))
                .andExpect(jsonPath("$.participantes.length()").value(1));
        mvc.perform(get("/api/inventario/" + carpa)).andExpect(jsonPath("$.cantidadReservada").value(0));
        mvc.perform(get("/api/inventario/" + soga)).andExpect(jsonPath("$.cantidadReservada").value(3));
    }

    @Test
    void elementosParaEditarNoDescuentanLaReservaDeLaPropiaSalida() throws Exception {
        int ruta = ruta(unico("Ruta elementos"));
        int carpa = elemento(unico("Carpa"), 5);
        int id = salida(ruta, "{\"elementoId\":%d,\"cantidad\":4}".formatted(carpa));

        mvc.perform(get("/api/inventario/" + carpa)).andExpect(jsonPath("$.cantidadDisponible").value(1));
        mvc.perform(get("/api/salidas/" + id + "/elementos-para-editar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==%d)].cantidadDisponible".formatted(carpa)).value(5))
                .andExpect(jsonPath("$[?(@.id==%d)].cantidadReservada".formatted(carpa)).value(0));
        mvc.perform(get("/api/salidas/999999/elementos-para-editar")).andExpect(status().isNotFound());
    }

    @Test
    void editarUnaSalidaRealizadaOCanceladaResponde409() throws Exception {
        int ruta = ruta(unico("Ruta final"));
        int cancelada = salida(ruta, "");
        int realizada = salida(ruta, "");
        salidas.cancelar(cancelada);
        salidas.marcarRealizada(realizada);

        editar(cancelada, cuerpo(ruta, cp("A") + "," + cp("B"), "", "")).andExpect(status().isConflict());
        editar(realizada, cuerpo(ruta, cp("A") + "," + cp("B"), "", "")).andExpect(status().isConflict());
    }

    @Test
    void faltaDeStockResponde409SinCambiosParciales() throws Exception {
        int ruta = ruta(unico("Ruta stock"));
        int carpa = elemento(unico("Carpa"), 5);
        int escaso = elemento(unico("Escaso"), 2);
        int id = salida(ruta, "{\"elementoId\":%d,\"cantidad\":2}".formatted(carpa));

        editar(id, cuerpo(ruta, cp("A") + "," + cp("B"),
                "{\"elementoId\":%d,\"cantidad\":3},{\"elementoId\":%d,\"cantidad\":3}".formatted(carpa, escaso), ""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"))
                .andExpect(jsonPath("$.disponible").value(2));

        mvc.perform(get("/api/salidas/" + id)).andExpect(jsonPath("$.fecha").value("2031-03-01"));
        mvc.perform(get("/api/inventario/" + carpa)).andExpect(jsonPath("$.cantidadReservada").value(2));
        mvc.perform(get("/api/inventario/" + escaso)).andExpect(jsonPath("$.cantidadReservada").value(0));
    }

    @Test
    void datosInvalidosResponden422ConErroresPorCampo() throws Exception {
        int ruta = ruta(unico("Ruta invalida"));
        int id = salida(ruta, "");

        editar(id, "{\"rutaId\":%d,\"checkpoints\":[%s,%s]}".formatted(ruta, cp("A"), cp("B")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("fecha")));
        editar(id, "{\"fecha\":\"2032-05-10\",\"checkpoints\":[]}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("rutaId")));
        editar(id, cuerpo(ruta, "{\"nombre\":\"\"}", "", ""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("checkpoints")));
        mvc.perform(get("/api/salidas/" + id + "/copia-operativa"))
                .andExpect(jsonPath("$.checkpoints[*].nombre", not(hasItem(""))));
    }

    @Test
    void salidaOCosaInexistenteResponde404() throws Exception {
        editar(999_999, cuerpo(1, cp("A") + "," + cp("B"), "", "")).andExpect(status().isNotFound());

        int ruta = ruta(unico("Ruta 404"));
        int id = salida(ruta, "");
        editar(id, cuerpo(ruta, cp("A") + "," + cp("B"), "", "999999")).andExpect(status().isNotFound());
    }
}
