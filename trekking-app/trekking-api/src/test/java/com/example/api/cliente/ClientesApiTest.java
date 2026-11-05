package com.example.api.cliente;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.example.api.PruebaApi;
import com.example.core.cliente.CasosDeUsoClientes;
import com.example.core.cliente.DatosCliente;

class ClientesApiTest extends PruebaApi {

    private static final AtomicInteger SECUENCIA = new AtomicInteger(30_000_000);

    @Autowired
    CasosDeUsoClientes clientes;

    private static String dniUnico() {
        return String.valueOf(SECUENCIA.incrementAndGet());
    }

    private static String json(String dni, String nombre, LocalDate nacimiento) {
        return """
                {"dni":"%s","nombre":"%s","apellido":"Prueba","fechaNacimiento":"%s","email":"a@b.com",
                 "telefono":"1155551234","sexo":"F","autorizacionMenores":false}
                """.formatted(dni, nombre, nacimiento);
    }

    private int crear(String nombre, LocalDate nacimiento, boolean activo) {
        boolean menor = nacimiento.isAfter(LocalDate.now().minusYears(18));
        int id = clientes.alta(new DatosCliente(dniUnico(), nombre, "Prueba", nacimiento, null, "1155551234", "F",
                null, null, null, menor, menor ? "Tutor" : null, menor ? "Prueba" : null,
                menor ? "20123456" : null, menor ? "1155550000" : null)).id();
        if (!activo) clientes.darDeBaja(id);
        return id;
    }

    @Test
    void creaUnClienteYResponde201() throws Exception {
        String dni = dniUnico();
        mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                .content(json(dni, "Creada", LocalDate.of(1990, 5, 1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.dni").value(dni))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.menor").value(false));
    }

    @Test
    void datosInvalidosResponden422ConErroresPorCampo() throws Exception {
        mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                .content(json("abc", "Mala", LocalDate.of(1990, 5, 1))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.errores[*].campo", hasItem("dni")));
    }

    @Test
    void detalleIndicaSiEsMenor() throws Exception {
        int menor = crear("Menor", LocalDate.now().minusYears(10), true);
        int mayor = crear("Mayor", LocalDate.of(1980, 1, 1), true);
        mvc.perform(get("/api/clientes/" + menor)).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Menor")).andExpect(jsonPath("$.menor").value(true));
        mvc.perform(get("/api/clientes/" + mayor)).andExpect(jsonPath("$.menor").value(false));
    }

    @Test
    void listaFiltraPorEstadoConActivasPorDefecto() throws Exception {
        crear("ListaActiva", LocalDate.of(1980, 1, 1), true);
        crear("ListaBaja", LocalDate.of(1980, 1, 1), false);
        mvc.perform(get("/api/clientes"))
                .andExpect(jsonPath("$[*].nombre", hasItem("ListaActiva")))
                .andExpect(jsonPath("$[*].nombre", not(hasItem("ListaBaja"))));
        mvc.perform(get("/api/clientes").param("estado", "bajas"))
                .andExpect(jsonPath("$[*].nombre", hasItem("ListaBaja")))
                .andExpect(jsonPath("$[*].nombre", not(hasItem("ListaActiva"))));
        mvc.perform(get("/api/clientes").param("estado", "todas"))
                .andExpect(jsonPath("$[*].nombre", hasItem("ListaActiva")))
                .andExpect(jsonPath("$[*].nombre", hasItem("ListaBaja")));
    }

    @Test
    void editaUnCliente() throws Exception {
        int id = crear("Original", LocalDate.of(1980, 1, 1), true);
        String dni = clientes.obtener(id).dni();
        mvc.perform(put("/api/clientes/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(json(dni, "Editado", LocalDate.of(1980, 1, 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Editado"));
    }

    @Test
    void editarConDatosInvalidosResponde422() throws Exception {
        int id = crear("EditInvalido", LocalDate.of(1980, 1, 1), true);
        mvc.perform(put("/api/clientes/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(json("1", "X", LocalDate.of(1980, 1, 1))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("dni")));
    }

    @Test
    void validaSinGuardar() throws Exception {
        String dni = dniUnico();
        mvc.perform(post("/api/clientes/validar").contentType(MediaType.APPLICATION_JSON)
                .content(json(dni, "Vivo", LocalDate.of(1990, 5, 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(post("/api/clientes/validar").contentType(MediaType.APPLICATION_JSON)
                .content(json("abc", "Vivo", LocalDate.of(1990, 5, 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].campo", hasItem("dni")));
        mvc.perform(get("/api/clientes").param("estado", "todas"))
                .andExpect(jsonPath("$[*].dni", not(hasItem(dni))));
    }

    @Test
    void daDeBajaYReactiva() throws Exception {
        int id = crear("Ciclo", LocalDate.of(1980, 1, 1), true);
        mvc.perform(post("/api/clientes/" + id + "/baja")).andExpect(status().isNoContent());
        mvc.perform(get("/api/clientes/" + id)).andExpect(jsonPath("$.activo").value(false));
        mvc.perform(post("/api/clientes/" + id + "/reactivacion")).andExpect(status().isNoContent());
        mvc.perform(get("/api/clientes/" + id)).andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void noExisteDelete() throws Exception {
        int id = crear("SinDelete", LocalDate.of(1980, 1, 1), true);
        mvc.perform(delete("/api/clientes/" + id)).andExpect(status().is(org.hamcrest.Matchers.not(org.hamcrest.Matchers.lessThan(300))));
        mvc.perform(get("/api/clientes/" + id)).andExpect(status().isOk());
    }

    @Test
    void clienteInexistenteResponde404() throws Exception {
        mvc.perform(get("/api/clientes/999999")).andExpect(status().isNotFound());
        mvc.perform(put("/api/clientes/999999").contentType(MediaType.APPLICATION_JSON)
                .content(json(dniUnico(), "N", LocalDate.of(1990, 5, 1)))).andExpect(status().isNotFound());
        mvc.perform(post("/api/clientes/999999/baja")).andExpect(status().isNotFound());
        mvc.perform(post("/api/clientes/999999/reactivacion")).andExpect(status().isNotFound());
    }
}
