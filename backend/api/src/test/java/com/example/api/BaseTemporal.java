package com.example.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/** Crea archivos de base de datos SQLite en directorios temporales que se borran al terminar la JVM. */
public final class BaseTemporal {

    private BaseTemporal() {
    }

    /** Ruta de un {@code trekking.db} nuevo dentro de un directorio temporal propio. */
    public static Path nueva(String prefijo) {
        try {
            Path directorio = Files.createTempDirectory(prefijo);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> borrar(directorio)));
            return directorio.resolve("trekking.db");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void borrar(Path directorio) {
        try (Stream<Path> archivos = Files.walk(directorio)) {
            archivos.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignorada) {
            // limpieza de mejor esfuerzo
        }
    }
}
