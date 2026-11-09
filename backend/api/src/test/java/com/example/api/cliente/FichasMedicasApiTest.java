package com.example.api.cliente;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import com.example.api.PruebaApi;
import com.example.core.cliente.CasosDeUsoClientes;
import com.example.core.cliente.DatosCliente;

class FichasMedicasApiTest extends PruebaApi {

    private static final AtomicInteger SECUENCIA = new AtomicInteger(40_000_000);
    private static final int MAXIMO = 10 * 1024 * 1024;

    @Autowired
    CasosDeUsoClientes clientes;

    private int crearCliente() {
        return clientes.alta(new DatosCliente(String.valueOf(SECUENCIA.incrementAndGet()), "Ficha", "Prueba",
                LocalDate.of(1990, 1, 1), null, "1155551234", "F", null, null, null, false, null, null, null, null))
                .id();
    }

    private static MockMultipartFile archivo(String nombre, byte[] contenido) {
        return new MockMultipartFile("archivo", nombre, "application/pdf", contenido);
    }

    private ResultActions subir(int id, MockMultipartFile f) throws Exception {
        return mvc.perform(multipart(HttpMethod.PUT, "/api/clientes/{id}/ficha-medica", id).file(f));
    }

    @Test
    void adjuntaYDescargaConSuNombreOriginal() throws Exception {
        int id = crearCliente();
        subir(id, archivo("ficha uno.pdf", "uno".getBytes())).andExpect(status().isNoContent());

        mvc.perform(get("/api/clientes/{id}/ficha-medica", id))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("ficha%20uno.pdf")))
                .andExpect(content().bytes("uno".getBytes()));
    }

    @Test
    void reemplazaLaFichaExistente() throws Exception {
        int id = crearCliente();
        subir(id, archivo("a.pdf", "viejo".getBytes())).andExpect(status().isNoContent());
        subir(id, archivo("b.pdf", "nuevo".getBytes())).andExpect(status().isNoContent());

        mvc.perform(get("/api/clientes/{id}/ficha-medica", id))
                .andExpect(header().string("Content-Disposition", containsString("b.pdf")))
                .andExpect(content().bytes("nuevo".getBytes()));
    }

    @Test
    void sinFichaLaDescargaResponde404() throws Exception {
        mvc.perform(get("/api/clientes/{id}/ficha-medica", crearCliente()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    void quitaLaFichaYQuitarSinFichaNoEsError() throws Exception {
        int id = crearCliente();
        subir(id, archivo("a.pdf", "x".getBytes())).andExpect(status().isNoContent());

        mvc.perform(delete("/api/clientes/{id}/ficha-medica", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/clientes/{id}/ficha-medica", id)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/clientes/{id}/ficha-medica", id)).andExpect(status().isNoContent());
    }

    @Test
    void clienteInexistenteResponde404EnLosTresVerbos() throws Exception {
        subir(999_999_999, archivo("a.pdf", "x".getBytes())).andExpect(status().isNotFound());
        mvc.perform(get("/api/clientes/{id}/ficha-medica", 999_999_999)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/clientes/{id}/ficha-medica", 999_999_999)).andExpect(status().isNotFound());
    }

    @Test
    void archivoQueSuperaElMaximoSeRechazaConUnErrorClaro() throws Exception {
        int id = crearCliente();
        subir(id, archivo("grande.pdf", new byte[MAXIMO + 1]))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.codigo").value("ARCHIVO_DEMASIADO_GRANDE"))
                .andExpect(jsonPath("$.mensaje").isString());
        mvc.perform(get("/api/clientes/{id}/ficha-medica", id)).andExpect(status().isNotFound());
    }

    @Test
    void unArchivoDeExactamenteElMaximoEsValido() throws Exception {
        subir(crearCliente(), archivo("justo.pdf", new byte[MAXIMO])).andExpect(status().isNoContent());
    }
}
