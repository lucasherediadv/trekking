package com.example.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.core.excepcion.EntidadNoEncontradaException;
import com.example.core.ruta.DatosRuta;
import com.example.core.ruta.Ruta;

class RepositorioRutasSqliteTest {

    @TempDir
    Path directorio;

    private BaseDatosSqlite base;
    private RepositorioRutasSqlite repositorio;

    @BeforeEach
    void preparar() {
        base = new BaseDatosSqlite(directorio.resolve("rutas.db"));
        base.inicializarEsquema();
        repositorio = new RepositorioRutasSqlite(base);
    }

    private static DatosRuta datos(String nombre) {
        return new DatosRuta(nombre, 1250.5, "Rocoso", "Media", "Alta");
    }

    private void agregarCheckpoints(int rutaId, int cantidad) throws SQLException {
        try (Connection c = base.conectar(); Statement s = c.createStatement()) {
            for (int i = 1; i <= cantidad; i++) {
                s.executeUpdate("INSERT INTO checkpoints (nombre, hora, latitud, longitud, altitud) VALUES ('P"
                        + rutaId + "-" + i + "', '08:00', 0, 0, 0)");
                s.executeUpdate("INSERT INTO ruta_checkpoints (ruta_id, checkpoint_id, orden) VALUES (" + rutaId
                        + ", (SELECT MAX(id) FROM checkpoints), " + i + ")");
            }
        }
    }

    @Test
    void guardaYRecuperaUnaRutaActivaSinCheckpoints() {
        int id = repositorio.insertar(datos("  Cerro   Uritorco "));

        assertEquals(new Ruta(id, "Cerro Uritorco", 1250.5, "Rocoso", "Media", "Alta", true, 0),
                repositorio.buscarPorId(id).orElseThrow());
        assertTrue(repositorio.buscarPorId(id + 1).isEmpty());
    }

    @Test
    void cuentaLosCheckpointsDelItinerarioGuardado() throws SQLException {
        int id = repositorio.insertar(datos("Cerro Uritorco"));
        agregarCheckpoints(id, 3);

        Ruta ruta = repositorio.buscarPorId(id).orElseThrow();

        assertEquals(3, ruta.cantidadCheckpoints());
        assertFalse(ruta.esBorrador());
    }

    @Test
    void actualizaLosDatosYLaClaveDelNombre() {
        int id = repositorio.insertar(datos("Cerro Uritorco"));

        repositorio.actualizar(id, new DatosRuta("Cerro Champaquí", 2790, "Mixto", "Baja", "Baja"));

        assertEquals("Cerro Champaquí", repositorio.buscarPorId(id).orElseThrow().nombre());
        assertTrue(repositorio.existeNombre(" cerro  CHAMPAQUÍ", -1));
        assertFalse(repositorio.existeNombre("Cerro Uritorco", -1));
    }

    @Test
    void existeNombreIgnoraMayusculasEspaciosYLaRutaExcluida() {
        int id = repositorio.insertar(datos("Cerro Uritorco"));

        assertTrue(repositorio.existeNombre("  cerro   URITORCO ", -1));
        assertFalse(repositorio.existeNombre("cerro uritorco", id));
    }

    @Test
    void bajaYReactivacionMuevenLaRutaEntreListados() {
        int id = repositorio.insertar(datos("Cerro Uritorco"));
        int otra = repositorio.insertar(datos("Cerro Champaquí"));

        repositorio.cambiarActiva(id, false);

        assertEquals(List.of(otra), repositorio.listarActivas().stream().map(Ruta::id).toList());
        assertEquals(List.of(id), repositorio.listarDadasDeBaja().stream().map(Ruta::id).toList());
        assertEquals(2, repositorio.listarTodas().size());

        repositorio.cambiarActiva(id, true);
        assertEquals(2, repositorio.listarActivas().size());
    }

    @Test
    void operarSobreUnaRutaInexistenteFalla() {
        assertThrows(EntidadNoEncontradaException.class, () -> repositorio.cambiarActiva(99, false));
        assertThrows(EntidadNoEncontradaException.class, () -> repositorio.actualizar(99, datos("X")));
    }
}
