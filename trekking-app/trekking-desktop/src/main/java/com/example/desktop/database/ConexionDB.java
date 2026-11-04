package com.example.desktop.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

import com.example.sqlite.BaseDatosSqlite;

/**
 * Acceso de la app de escritorio a la base SQLite. Resuelve la ubicación del archivo
 * y delega la conexión y el esquema en {@link BaseDatosSqlite}.
 */
public final class ConexionDB {

    private static final BaseDatosSqlite BASE = new BaseDatosSqlite(rutaArchivoBase());

    private ConexionDB() {
    }

    /**
     * Usa la propiedad de sistema {@code trekking.db}; si no está, busca hacia arriba la raíz
     * del repositorio (la carpeta que contiene {@code trekking-app/pom.xml}).
     */
    private static Path rutaArchivoBase() {
        String configurada = System.getProperty("trekking.db");
        if (configurada != null && !configurada.isBlank()) return Path.of(configurada);

        for (Path directorio = Path.of("").toAbsolutePath().normalize(); directorio != null; directorio = directorio.getParent()) {
            if (Files.isRegularFile(directorio.resolve("trekking-app/pom.xml"))) {
                return directorio.resolve("trekking.db");
            }
        }
        return Path.of("trekking.db");
    }

    /** Initializes the database without starting the JavaFX application. */
    public static void main(String[] args) {
        crearTabla();
        System.out.println("Esquema de trekking listo.");
    }

    /** Base compartida por los repositorios de sqlite. */
    public static BaseDatosSqlite base() {
        return BASE;
    }

    public static Connection conectar() throws SQLException {
        return BASE.conectar();
    }

    public static void crearTabla() {
        BASE.inicializarEsquema();
    }
}
