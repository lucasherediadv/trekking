package com.example.desktop.controller;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class FichaMedica {
    private static final File CARPETA = new File("fichas_medicas");

    private final byte[] contenido;
    private final String nombre;

    public FichaMedica(byte[] contenido, String nombre) {
        this.contenido = contenido;
        this.nombre = nombre;
    }

    public byte[] getContenido() {
        return contenido;
    }

    public String getNombre() {
        return nombre;
    }

    public static void guardarEnCarpeta(String dni, byte[] contenido) throws IOException {
        if (!CARPETA.exists() && !CARPETA.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta fichas_medicas");
        }
        Files.write(new File(CARPETA, dni + ".pdf").toPath(), contenido);
    }

    public static void borrarDeCarpeta(String dni) throws IOException {
        Files.deleteIfExists(new File(CARPETA, dni + ".pdf").toPath());
    }

    public static void renombrarEnCarpeta(
            String dniActual,
            String dniNuevo) throws IOException {
        File archivoActual = new File(CARPETA, dniActual + ".pdf");
        if (archivoActual.isFile()) {
            File archivoNuevo = new File(CARPETA, dniNuevo + ".pdf");
            Files.move(
                    archivoActual.toPath(),
                    archivoNuevo.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void abrirDesdeCarpeta(String dni) throws IOException {
        File archivo = new File(CARPETA, dni + ".pdf");
        if (!archivo.isFile()) {
            throw new IOException("No existe la ficha medica para el DNI " + dni + ".");
        }
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            throw new IOException("El sistema no tiene disponible un visor de PDF.");
        }
        Desktop.getDesktop().open(archivo);
    }
}