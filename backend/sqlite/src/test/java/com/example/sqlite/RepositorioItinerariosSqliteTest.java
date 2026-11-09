package com.example.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.core.excepcion.EntidadNoEncontradaException;
import com.example.core.excepcion.ValidacionException;
import com.example.core.itinerario.CasosDeUsoItinerario;
import com.example.core.itinerario.Checkpoint;
import com.example.core.itinerario.Itinerario;
import com.example.core.ruta.CasosDeUsoRutas;
import com.example.core.ruta.DatosRuta;

class RepositorioItinerariosSqliteTest {

    @TempDir
    Path directorio;

    private CasosDeUsoRutas rutas;
    private CasosDeUsoItinerario itinerarios;
    private RepositorioItinerariosSqlite repositorio;
    private int rutaId;

    @BeforeEach
    void preparar() {
        BaseDatosSqlite base = new BaseDatosSqlite(directorio.resolve("itinerarios.db"));
        base.inicializarEsquema();
        RepositorioRutasSqlite repoRutas = new RepositorioRutasSqlite(base);
        repositorio = new RepositorioItinerariosSqlite(base);
        rutas = new CasosDeUsoRutas(repoRutas);
        itinerarios = new CasosDeUsoItinerario(repoRutas, repositorio);
        rutaId = rutas.alta(new DatosRuta("Cerro Uritorco", 1000, "Rocoso", "Media", "Alta")).id();
    }

    private static Checkpoint cp(String nombre, String hora) {
        return new Checkpoint(0, nombre, hora, -31.4, -64.2, 100, "desc");
    }

    private List<String> nombres() {
        return itinerarios.obtener(rutaId).checkpoints().stream().map(Checkpoint::nombre).toList();
    }

    @Test
    void guardaElItinerarioConSuOrdenYLoDevuelveIgual() {
        itinerarios.guardar(rutaId, Itinerario.vacio().agregar(cp("B", "09:00")).agregar(cp("A", "09:00"))
                .agregar(cp("Inicio", "08:00")));

        assertEquals(List.of("Inicio", "B", "A"), nombres());
        assertEquals(3, rutas.obtener(rutaId).cantidadCheckpoints());
        assertTrue(rutas.obtener(rutaId).puedeUsarseEnSalida());
    }

    @Test
    void reemplazarActualizaConservaIdsYEliminaLosQuitados() {
        itinerarios.guardar(rutaId, Itinerario.vacio().agregar(cp("A", "08:00")).agregar(cp("B", "09:00"))
                .agregar(cp("C", "10:00")));
        Itinerario guardado = itinerarios.obtener(rutaId);
        long idA = guardado.checkpoints().get(0).id();

        Itinerario nuevo = guardado.quitar(2).reemplazar(0, guardado.checkpoints().get(0).conId(idA));
        Checkpoint editado = new Checkpoint(idA, "A2", "08:00", 0, 0, 5, "");
        itinerarios.guardar(rutaId, nuevo.reemplazar(0, editado));

        Itinerario releido = itinerarios.obtener(rutaId);
        assertEquals(List.of("A2", "B"), releido.checkpoints().stream().map(Checkpoint::nombre).toList());
        assertEquals(idA, releido.checkpoints().get(0).id());
    }

    @Test
    void unItinerarioInvalidoNoCambiaLoGuardado() {
        itinerarios.guardar(rutaId, Itinerario.vacio().agregar(cp("A", "08:00")).agregar(cp("B", "09:00")));

        assertThrows(ValidacionException.class,
                () -> itinerarios.guardar(rutaId, Itinerario.vacio().agregar(cp("Solo", "08:00"))));
        assertEquals(List.of("A", "B"), nombres());
    }

    @Test
    void unIdAjenoALaRutaHaceRevertirTodo() {
        itinerarios.guardar(rutaId, Itinerario.vacio().agregar(cp("A", "08:00")).agregar(cp("B", "09:00")));

        assertThrows(EntidadNoEncontradaException.class, () -> repositorio.reemplazar(rutaId,
                List.of(cp("Nuevo", "07:00"), new Checkpoint(9999, "Ajeno", "08:00", 0, 0, 0, ""))));
        assertEquals(List.of("A", "B"), nombres());
    }

    @Test
    void vaciarElItinerarioEliminaLosCheckpoints() {
        itinerarios.guardar(rutaId, Itinerario.vacio().agregar(cp("A", "08:00")).agregar(cp("B", "09:00")));

        itinerarios.guardar(rutaId, Itinerario.vacio());

        assertTrue(nombres().isEmpty());
        assertFalse(rutas.obtener(rutaId).puedeUsarseEnSalida());
        assertTrue(rutas.obtener(rutaId).esBorrador());
    }
}
