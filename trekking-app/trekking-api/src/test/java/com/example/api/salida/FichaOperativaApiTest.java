package com.example.api.salida;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.example.api.PruebaApi;
import com.example.core.itinerario.CasosDeUsoItinerario;
import com.example.core.itinerario.Checkpoint;
import com.example.core.itinerario.Itinerario;
import com.example.core.ruta.CasosDeUsoRutas;
import com.example.core.ruta.DatosRuta;

/** Descarga de la Ficha operativa: {@code GET /api/salidas/{id}/ficha-operativa}. */
class FichaOperativaApiTest extends PruebaApi {

    private static final AtomicInteger DNI = new AtomicInteger(70_000_000 + ThreadLocalRandom.current().nextInt(9_000_000));
    private static final String MARCADOR = "MARCADORFICHAMEDICASECRETO";

    @Autowired
    CasosDeUsoRutas rutas;
    @Autowired
    CasosDeUsoItinerario itinerarios;

    private int salidaConParticipante(String nombreRuta, String nombreCliente) throws Exception {
        int ruta = rutas.alta(new DatosRuta(nombreRuta, 4200, "Rocoso", "Media", "Alta")).id();
        itinerarios.guardar(ruta, Itinerario.de(List.of(
                new Checkpoint(0, "Base", "06:00", -33.0, -69.0, 3000, "Inicio"),
                new Checkpoint(0, "Cumbre", "11:00", -33.1, -69.1, 4000, "Fin"))));
        String ficha = Base64.getEncoder().encodeToString(
                (MARCADOR + " diagnostico reservado").getBytes(StandardCharsets.UTF_8));
        String cuerpo = """
                {"fecha":"%s","rutaId":%d,"equipamiento":[],"elementosNuevos":[],"clientesExistentes":[],
                "clientesNuevos":[{"cliente":{"dni":"%s","nombre":"%s","apellido":"Participante",
                "fechaNacimiento":"1991-02-03","telefono":"3511234567","sexo":"Masculino",
                "autorizacionMenores":false},"ficha":{"nombreArchivo":"ficha.txt","contenidoBase64":"%s"}}]}
                """.formatted(LocalDate.now().plusYears(5), ruta, DNI.incrementAndGet(), nombreCliente, ficha);
        String respuesta = mvc.perform(post("/api/salidas").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return Integer.parseInt(respuesta.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    /** Nombres de Cliente: solo letras. */
    private static String letras(String base) {
        StringBuilder s = new StringBuilder(base).append(' ');
        for (char c : Long.toString(System.nanoTime()).toCharArray()) s.append((char) ('a' + (c - '0')));
        return s.toString();
    }

    @Test
    void devuelveUnPdfValidoConCabecerasDeDescarga() throws Exception {
        String nombreRuta = "Ruta Ficha " + System.nanoTime();
        int id = salidaConParticipante(nombreRuta, letras("Pdf"));

        byte[] pdf = mvc.perform(get("/api/salidas/" + id + "/ficha-operativa"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string("Content-Disposition",
                        allOf(startsWith("attachment"), containsString("salida-" + id + ".pdf"))))
                .andReturn().getResponse().getContentAsByteArray();

        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertTrue(doc.getNumberOfPages() >= 1);
            assertTrue(new PDFTextStripper().getText(doc).contains(nombreRuta));
        }
    }

    @Test
    void elPdfNoIncorporaLaFichaMedicaDeLosParticipantes() throws Exception {
        String nombre = letras("ConFicha");
        int id = salidaConParticipante("Ruta Privada " + System.nanoTime(), nombre);

        byte[] pdf = mvc.perform(get("/api/salidas/" + id + "/ficha-operativa"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();

        try (PDDocument doc = Loader.loadPDF(pdf)) {
            String texto = new PDFTextStripper().getText(doc);
            assertTrue(texto.contains(nombre), "el participante sí figura en la ficha operativa");
            assertFalse(texto.contains(MARCADOR));
        }
        assertFalse(new String(pdf, StandardCharsets.ISO_8859_1).contains(MARCADOR));
    }

    @Test
    void salidaInexistenteResponde404() throws Exception {
        mvc.perform(get("/api/salidas/999999/ficha-operativa")).andExpect(status().isNotFound());
    }
}
