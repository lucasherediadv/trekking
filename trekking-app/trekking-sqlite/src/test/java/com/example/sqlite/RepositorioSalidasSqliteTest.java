package com.example.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.core.inventario.CasosDeUsoInventario;
import com.example.core.inventario.DatosElemento;
import com.example.core.inventario.EstadoElemento;
import com.example.core.itinerario.Checkpoint;
import com.example.core.salida.CasosDeUsoSalidas;
import com.example.core.salida.CopiaOperativa;
import com.example.core.salida.EstadoSalida;
import com.example.core.salida.TransicionInvalidaException;

class RepositorioSalidasSqliteTest {

    @TempDir
    Path directorio;

    private BaseDatosSqlite base;
    private CasosDeUsoSalidas salidas;
    private CasosDeUsoInventario inventario;
    private int elementoId;

    @BeforeEach
    void preparar() throws Exception {
        base = new BaseDatosSqlite(directorio.resolve("salidas.db"));
        base.inicializarEsquema();
        Clock reloj = Clock.fixed(Instant.parse("2026-05-10T15:00:00Z"), ZoneOffset.UTC);
        salidas = new CasosDeUsoSalidas(new RepositorioSalidasSqlite(base), reloj);
        inventario = new CasosDeUsoInventario(new RepositorioInventarioSqlite(base));
        elementoId = inventario.alta(new DatosElemento("Soga", "Seguridad", 10, EstadoElemento.DISPONIBLE)).id();
        ejecutar("INSERT INTO rutas (id, nombre, nombre_clave, altitud_maxima, tipo_terreno, dificultad_tecnica, dificultad_fisica, activo)"
                + " VALUES (1, 'Uritorco', 'uritorco', 1000, 'Rocoso', 'Media', 'Alta', 1)");
        ejecutar("INSERT INTO clientes (id, dni, nombre, apellido, sexo, activo) VALUES (1, '1', 'Ana', 'Paz', 'Femenino', 1)");
        insertarSalida(1, "2026-05-01");
        insertarSalida(2, "2026-05-10");
        ejecutar("INSERT INTO salida_equipamiento VALUES (1, " + elementoId + ", 'Soga', 'Seguridad', 4)");
        ejecutar("INSERT INTO salida_participantes (salida_id, cliente_id, dni_copia, nombre_copia, apellido_copia)"
                + " VALUES (1, 1, '1', 'Ana', 'Paz')");
    }

    private void insertarSalida(int id, String fecha) throws Exception {
        ejecutar("INSERT INTO salidas (id, ruta_id, fecha, ruta_nombre, ruta_altitud_maxima, ruta_tipo_terreno,"
                + " ruta_dificultad_tecnica, ruta_dificultad_fisica) VALUES (" + id + ", 1, '" + fecha
                + "', 'Uritorco', 1000, 'Rocoso', 'Media', 'Alta')");
    }

    private void ejecutar(String sql) throws Exception {
        try (Connection c = base.conectar(); Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }

    private String estadoGuardado(int id) throws Exception {
        try (Connection c = base.conectar(); Statement s = c.createStatement();
                ResultSet r = s.executeQuery("SELECT estado FROM salidas WHERE id = " + id)) {
            r.next();
            return r.getString(1);
        }
    }

    private int reservada() {
        return inventario.obtener(elementoId).cantidadReservada();
    }

    @Test
    void listaConResumenesYVencida() {
        var filas = salidas.listar();
        assertEquals(2, filas.size());
        assertEquals(2, filas.get(0).salida().id());
        assertEquals(false, filas.get(0).vencida());
        assertEquals(true, filas.get(1).vencida());
        assertEquals("Ana Paz", filas.get(1).resumen().participantes());
        assertEquals("Soga × 4", filas.get(1).resumen().equipamiento());
        assertEquals("", filas.get(0).resumen().equipamiento());
    }

    @Test
    void cerrarGuardaElMismoTextoYLiberaLaReserva() throws Exception {
        assertEquals(4, reservada());
        salidas.marcarRealizada(1);
        assertEquals("realizada", estadoGuardado(1));
        assertEquals(EstadoSalida.REALIZADA, salidas.obtener(1).estado());
        assertEquals(0, reservada());
    }

    @Test
    void cancelarGuardaElTextoYNoSePuedeCerrarDeNuevo() throws Exception {
        salidas.cancelar(2);
        assertEquals("cancelada", estadoGuardado(2));
        assertThrows(TransicionInvalidaException.class, () -> salidas.marcarRealizada(2));
        assertEquals("cancelada", estadoGuardado(2));
    }

    @Test
    void reemplazarLaCopiaOperativaNoDejaVersionesAnteriores() throws Exception {
        ejecutar("INSERT INTO salida_checkpoints (salida_id, nombre, hora, latitud, longitud, altitud, descripcion, orden)"
                + " VALUES (1, 'Viejo', '08:00', 0, 0, 100, '', 0)");
        var repositorio = new RepositorioSalidasSqlite(base);
        var copia = repositorio.buscarCopia(1).orElseThrow();
        assertEquals("Soga", copia.equipamiento().get(0).elemento().nombre());
        assertEquals("Ana", copia.participantes().get(0).nombre());
        assertEquals(List.of("Viejo"), copia.checkpoints().stream().map(Checkpoint::nombre).toList());

        var reemplazo = new CopiaOperativa(LocalDate.of(2026, 6, 1), copia.ruta(),
                List.of(new Checkpoint(0, "Nuevo", "09:00", 1, 2, 300, "d")),
                List.of(new CopiaOperativa.LineaEquipamiento(copia.equipamiento().get(0).elemento(), 7)), List.of());
        repositorio.reemplazarCopia(1, reemplazo);

        var leida = repositorio.buscarCopia(1).orElseThrow();
        assertEquals(LocalDate.of(2026, 6, 1), leida.fecha());
        assertEquals(List.of("Nuevo"), leida.checkpoints().stream().map(Checkpoint::nombre).toList());
        assertEquals(7, leida.equipamiento().get(0).cantidad());
        assertEquals(0, leida.participantes().size());
        assertEquals(7, reservada());
        assertEquals(1, contar("salida_checkpoints"));
        assertEquals(1, contar("salida_equipamiento"));
        assertEquals(0, contar("salida_participantes"));
    }

    private int contar(String tabla) throws Exception {
        try (Connection c = base.conectar(); Statement s = c.createStatement();
                ResultSet r = s.executeQuery("SELECT COUNT(*) FROM " + tabla + " WHERE salida_id = 1")) {
            r.next();
            return r.getInt(1);
        }
    }
}
