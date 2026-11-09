package com.example.core.itinerario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.core.excepcion.EntidadNoEncontradaException;
import com.example.core.excepcion.ValidacionException;
import com.example.core.ruta.CasosDeUsoRutas;
import com.example.core.ruta.DatosRuta;
import com.example.core.ruta.RepositorioRutasEnMemoria;
import com.example.core.ruta.Ruta;

class CasosDeUsoItinerarioTest {

    private CasosDeUsoRutas rutas;
    private CasosDeUsoItinerario itinerarios;
    private int rutaId;

    @BeforeEach
    void preparar() {
        RepositorioRutasEnMemoria repoRutas = new RepositorioRutasEnMemoria();
        rutas = new CasosDeUsoRutas(repoRutas);
        itinerarios = new CasosDeUsoItinerario(repoRutas, new RepositorioItinerariosEnMemoria(repoRutas));
        rutaId = rutas.alta(new DatosRuta("Cerro Uritorco", 1000, "Rocoso", "Media", "Alta")).id();
    }

    private static Checkpoint cp(String nombre, String hora, double altitud) {
        return new Checkpoint(0, nombre, hora, -31.4, -64.2, altitud, "");
    }

    private List<String> nombres() {
        return itinerarios.obtener(rutaId).checkpoints().stream().map(Checkpoint::nombre).toList();
    }

    @Test
    void elItinerarioSeOrdenaPorHora() {
        itinerarios.guardar(rutaId, Itinerario.vacio()
                .agregar(cp("Cumbre", "11:00", 900)).agregar(cp("Base", "08:00", 100)).agregar(cp("Mirador", "09:30", 500)));

        assertEquals(List.of("Base", "Mirador", "Cumbre"), nombres());
    }

    @Test
    void conHorasIgualesConservaLaPosicionExplicita() {
        itinerarios.guardar(rutaId, Itinerario.vacio()
                .agregar(cp("B", "09:00", 100)).agregar(cp("A", "09:00", 100)).agregar(cp("Inicio", "08:00", 100)));

        assertEquals(List.of("Inicio", "B", "A"), nombres());
    }

    @Test
    void editarLaHoraReubicaElCheckpoint() {
        Itinerario it = Itinerario.vacio().agregar(cp("A", "08:00", 1)).agregar(cp("B", "09:00", 1));

        Itinerario editado = it.reemplazar(0, cp("A", "10:00", 1));

        assertEquals("B", editado.checkpoints().get(0).nombre());
        assertEquals(1, it.quitar(0).cantidad());
    }

    @Test
    void altitudMayorALaMaximaDeLaRutaSeRechazaYNoGuarda() {
        Itinerario it = Itinerario.vacio().agregar(cp("Base", "08:00", 100)).agregar(cp("Cumbre", "11:00", 1000.5));

        ValidacionException e = assertThrows(ValidacionException.class, () -> itinerarios.guardar(rutaId, it));

        assertTrue(e.getMessage().contains("Cumbre"));
        assertTrue(nombres().isEmpty());
    }

    @Test
    void altitudIgualALaMaximaSeAcepta() {
        itinerarios.guardar(rutaId, Itinerario.vacio().agregar(cp("Base", "08:00", 0)).agregar(cp("Cumbre", "11:00", 1000)));

        assertEquals(2, nombres().size());
    }

    @Test
    void guardarUnUnicoCheckpointSeRechaza() {
        assertThrows(ValidacionException.class,
                () -> itinerarios.guardar(rutaId, Itinerario.vacio().agregar(cp("Solo", "08:00", 10))));
        assertTrue(nombres().isEmpty());
    }

    @Test
    void checkpointInvalidoSeRechaza() {
        Itinerario it = Itinerario.vacio().agregar(cp("", "8:00", 10)).agregar(cp("Fin", "09:00", 10));

        assertThrows(ValidacionException.class, () -> itinerarios.guardar(rutaId, it));
    }

    @Test
    void conDosCheckpointsLaRutaDejaDeSerBorradorYPuedeUsarseEnUnaSalida() {
        assertTrue(rutas.obtener(rutaId).esBorrador());

        Ruta guardada = itinerarios.guardar(rutaId,
                Itinerario.vacio().agregar(cp("Base", "08:00", 100)).agregar(cp("Cumbre", "11:00", 900)));

        assertFalse(guardada.esBorrador());
        assertTrue(guardada.puedeUsarseEnSalida());
    }

    @Test
    void vaciarElItinerarioDevuelveLaRutaAlEstadoBorrador() {
        itinerarios.guardar(rutaId, Itinerario.vacio().agregar(cp("Base", "08:00", 100)).agregar(cp("Fin", "09:00", 200)));

        Ruta guardada = itinerarios.guardar(rutaId, Itinerario.vacio());

        assertTrue(guardada.esBorrador());
    }

    @Test
    void rutaInexistenteFalla() {
        assertThrows(EntidadNoEncontradaException.class, () -> itinerarios.obtener(999));
        assertThrows(EntidadNoEncontradaException.class, () -> itinerarios.guardar(999, Itinerario.vacio()));
    }

    @Test
    void validadorAceptaComaDecimalYRechazaFueraDeRango() {
        assertTrue(ValidadorCheckpoint.validar("A", "08:00", "-31,4", "-64,2", "10,5", 1000).isEmpty());
        assertEquals(List.of("hora", "latitud", "longitud", "altitud"),
                ValidadorCheckpoint.validar("A", "24:00", "91", "-181", "1001", 1000).stream()
                        .map(e -> e.campo()).toList());
    }
}
