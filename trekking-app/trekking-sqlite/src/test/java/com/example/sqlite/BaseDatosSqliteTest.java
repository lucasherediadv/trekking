package com.example.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BaseDatosSqliteTest {

    @TempDir
    Path directorio;

    @Test
    void inicializaElEsquemaEnElArchivoIndicado() throws SQLException {
        Path archivo = directorio.resolve("trekking.db");
        BaseDatosSqlite base = new BaseDatosSqlite(archivo);

        base.inicializarEsquema();

        assertTrue(Files.isRegularFile(archivo));
        assertTrue(tablas(base).containsAll(Set.of(
                "rutas", "checkpoints", "ruta_checkpoints", "equipamiento", "clientes", "sexos",
                "contactos_emergencia", "salidas", "salida_checkpoints", "salida_equipamiento",
                "salida_participantes")));
        assertEquals(5, contar(base, "SELECT COUNT(*) FROM sexos"));
    }

    @Test
    void noCreaLasTablasHeredadasDeRecorrido() throws SQLException {
        BaseDatosSqlite base = new BaseDatosSqlite(directorio.resolve("nueva.db"));

        base.inicializarEsquema();

        Set<String> tablas = tablas(base);
        assertFalse(tablas.contains("recorridos"));
        assertFalse(tablas.contains("recorrido_cliente"));
    }

    @Test
    void conservaLasTablasDeRecorridoDeUnaBaseExistente() throws SQLException {
        BaseDatosSqlite base = new BaseDatosSqlite(directorio.resolve("existente.db"));
        try (Connection conexion = base.conectar(); Statement statement = conexion.createStatement()) {
            statement.execute("CREATE TABLE recorridos (recorrido_id INTEGER PRIMARY KEY, fecha TEXT)");
            statement.execute("INSERT INTO recorridos (fecha) VALUES ('2026-01-01')");
        }

        base.inicializarEsquema();

        assertTrue(tablas(base).contains("recorridos"));
        assertEquals(1, contar(base, "SELECT COUNT(*) FROM recorridos"));
    }

    @Test
    void esIdempotenteYCompletaLaClaveDeNombreDeRutasExistentes() throws SQLException {
        BaseDatosSqlite base = new BaseDatosSqlite(directorio.resolve("rutas.db"));
        try (Connection conexion = base.conectar(); Statement statement = conexion.createStatement()) {
            statement.execute("""
                    CREATE TABLE rutas (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        nombre TEXT NOT NULL,
                        altitud_maxima REAL NOT NULL,
                        tipo_terreno TEXT NOT NULL,
                        dificultad_tecnica TEXT NOT NULL,
                        dificultad_fisica TEXT NOT NULL,
                        activo INTEGER NOT NULL DEFAULT 1)
                    """);
            statement.execute("""
                    INSERT INTO rutas (nombre, altitud_maxima, tipo_terreno, dificultad_tecnica, dificultad_fisica)
                    VALUES ('  Cerro   Uritorco ', 1979, 'Mixto', 'Media', 'Alta')
                    """);
        }

        base.inicializarEsquema();
        base.inicializarEsquema();

        try (Connection conexion = base.conectar(); Statement statement = conexion.createStatement();
                ResultSet fila = statement.executeQuery("SELECT nombre_clave FROM rutas")) {
            assertTrue(fila.next());
            assertEquals("cerro uritorco", fila.getString(1));
        }
        assertEquals(5, contar(base, "SELECT COUNT(*) FROM sexos"));
    }

    @Test
    void activaLasClavesForaneasEnCadaConexion() throws SQLException {
        BaseDatosSqlite base = new BaseDatosSqlite(directorio.resolve("fk.db"));

        assertEquals(1, contar(base, "PRAGMA foreign_keys"));
    }

    private static Set<String> tablas(BaseDatosSqlite base) throws SQLException {
        Set<String> tablas = new HashSet<>();
        try (Connection conexion = base.conectar(); Statement statement = conexion.createStatement();
                ResultSet filas = statement.executeQuery("SELECT name FROM sqlite_master WHERE type = 'table'")) {
            while (filas.next()) {
                tablas.add(filas.getString(1));
            }
        }
        return tablas;
    }

    private static int contar(BaseDatosSqlite base, String sql) throws SQLException {
        try (Connection conexion = base.conectar(); Statement statement = conexion.createStatement();
                ResultSet fila = statement.executeQuery(sql)) {
            return fila.next() ? fila.getInt(1) : 0;
        }
    }
}
