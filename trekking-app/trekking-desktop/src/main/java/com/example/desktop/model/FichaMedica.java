package com.example.desktop.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FichaMedica {

    private final byte[] datos;
    private final String nombreArchivo;

    public FichaMedica(byte[] datos, String nombreArchivo) {
        this.datos = datos;
        this.nombreArchivo = nombreArchivo;
    }

    public byte[] getDatos() {
        return datos;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void guardarEnCarpeta(File carpeta) throws IOException {

        if (!carpeta.exists()) {
            Files.createDirectories(carpeta.toPath());
        }

        Path destino = carpeta.toPath().resolve(nombreArchivo);

        Files.write(destino, datos);
    }

    public void guardarEnCarpeta(Path carpeta) throws IOException {

        if (!Files.exists(carpeta)) {
            Files.createDirectories(carpeta);
        }

        Path destino = carpeta.resolve(nombreArchivo);

        Files.write(destino, datos);
    }

    public void borrarDeCarpeta(File carpeta) throws IOException {

        if (carpeta == null || !carpeta.exists()) {
            return;
        }

        Path archivo = carpeta.toPath().resolve(nombreArchivo);

        Files.deleteIfExists(archivo);
    }

    public void borrarDeCarpeta(Path carpeta) throws IOException {

        if (carpeta == null || !Files.exists(carpeta)) {
            return;
        }

        Path archivo = carpeta.resolve(nombreArchivo);

        Files.deleteIfExists(archivo);
    }
}

