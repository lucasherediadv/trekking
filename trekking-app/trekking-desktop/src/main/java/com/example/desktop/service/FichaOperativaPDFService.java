package com.example.desktop.service;

import com.example.core.salida.Salida;
import com.example.desktop.Servicios;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Guarda en disco la Ficha operativa que genera core: propone el nombre y escribe de forma atómica. */
public class FichaOperativaPDFService {

    public String nombreArchivo(Salida salida) {
        String nombreRuta = salida.rutaNombre().trim().replaceAll("[^\\p{L}\\p{N}._-]+", "-")
                .replaceAll("^-+|-+$", "");
        if (nombreRuta.isBlank()) nombreRuta = "Ruta";
        return nombreRuta + "-" + salida.fecha() + "-salida-" + salida.id() + ".pdf";
    }

    /** @throws com.example.core.excepcion.EntidadNoEncontradaException la Salida ya no existe */
    public void exportar(int salidaId, Path destino) throws IOException {
        byte[] pdf = Servicios.fichaOperativa().generar(salidaId);
        Path absoluto = destino.toAbsolutePath();
        Path directorio = absoluto.getParent();
        if (directorio == null) throw new IOException("La ruta de destino no tiene una carpeta.");
        Path temporal = Files.createTempFile(directorio, "ficha-", ".tmp");
        try {
            Files.write(temporal, pdf);
            try {
                Files.move(temporal, absoluto, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, absoluto, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
    }
}
