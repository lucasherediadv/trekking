package com.example.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.core.inventario.CasosDeUsoInventario;
import com.example.core.inventario.DatosElemento;
import com.example.core.inventario.EstadoElemento;
import com.example.core.salida.CasosDeUsoSalidas;
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
}
