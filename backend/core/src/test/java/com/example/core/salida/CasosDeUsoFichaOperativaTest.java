package com.example.core.salida;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.core.cliente.Cliente;
import com.example.core.excepcion.EntidadNoEncontradaException;
import com.example.core.inventario.ElementoInventario;
import com.example.core.inventario.EstadoElemento;
import com.example.core.itinerario.Checkpoint;
import com.example.core.ruta.Ruta;

class CasosDeUsoFichaOperativaTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-04T15:00:00Z"), ZoneOffset.UTC);

    private RepositorioSalidasEnMemoria salidas;
    private CasosDeUsoFichaOperativa ficha;

    @BeforeEach
    void preparar() {
        salidas = new RepositorioSalidasEnMemoria();
        ficha = new CasosDeUsoFichaOperativa(salidas, RELOJ);
    }

    private static Ruta ruta() {
        return new Ruta(1, "Uritorco", 1000, "Rocoso", "Media", "Alta", true, 2);
    }

    private static Cliente cliente(int id, String apellido) {
        return new Cliente(id, "3011122" + id, "Ana", apellido, LocalDate.of(1990, 1, 1), null, "351111", "F",
                "Luis", "351222", "Padre", false, null, null, null, null, true, LocalDate.of(2026, 1, 1), null);
    }

    private static void assertEsPdf(byte[] bytes) {
        assertTrue(bytes.length > 0);
        assertEquals("%PDF-", new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII));
    }

    @Test
    void generaUnPdfParaUnaSalidaConDatos() {
        int id = salidas.guardar(new CopiaOperativa(LocalDate.of(2026, 12, 1), ruta(),
                List.of(new Checkpoint(1, "Inicio", "08:00", -31.4, -64.2, 500, "Base"),
                        new Checkpoint(2, "Cima", "11:00", -31.5, -64.3, 900, "")),
                List.of(new CopiaOperativa.LineaEquipamiento(
                        new ElementoInventario(1, "Soga", "Seguridad", 5, EstadoElemento.DISPONIBLE, 3), 3)),
                List.of(cliente(1, "Pérez"), cliente(2, "Álvarez"))));

        assertEsPdf(ficha.generar(id));
    }

    @Test
    void generaUnPdfParaUnaSalidaSinCheckpointsEquipamientoNiParticipantes() {
        int id = salidas.guardar(new CopiaOperativa(LocalDate.of(2026, 12, 1), ruta(), List.of(), List.of(), List.of()));

        assertEsPdf(ficha.generar(id));
    }

    @Test
    void generaUnPdfConMuchosDatosEnVariasPaginas() {
        List<Cliente> participantes = java.util.stream.IntStream.rangeClosed(1, 80)
                .mapToObj(i -> cliente(i, "Apellido" + i)).toList();
        int id = salidas.guardar(new CopiaOperativa(LocalDate.of(2026, 12, 1), ruta(), List.of(), List.of(), participantes));

        assertEsPdf(ficha.generar(id));
    }

    @Test
    void laFechaDeEmisionSaleDelRelojInyectado() throws Exception {
        int id = salidas.guardar(new CopiaOperativa(LocalDate.of(2026, 12, 1), ruta(), List.of(), List.of(), List.of()));
        CasosDeUsoFichaOperativa otroDia = new CasosDeUsoFichaOperativa(salidas,
                Clock.fixed(Instant.parse("2027-03-09T10:30:00Z"), ZoneOffset.UTC));

        assertTrue(texto(ficha.generar(id)).contains("Generada el 04/11/2026 15:00"));
        assertTrue(texto(otroDia.generar(id)).contains("Generada el 09/03/2027 10:30"));
    }

    private static String texto(byte[] pdf) throws java.io.IOException {
        try (org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.Loader.loadPDF(pdf)) {
            return new org.apache.pdfbox.text.PDFTextStripper().getText(doc);
        }
    }

    @Test
    void unaSalidaInexistenteEsEntidadNoEncontrada() {
        assertThrows(EntidadNoEncontradaException.class, () -> ficha.generar(99));
    }
}
