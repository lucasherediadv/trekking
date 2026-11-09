package com.example.core.ruta;

import java.util.Locale;

/** Reglas del nombre de una Ruta: normalización de espacios y clave única sin distinguir mayúsculas. */
public final class NombreRuta {

    private NombreRuta() {
    }

    /** Quita espacios en los extremos y colapsa los espacios internos en uno solo. */
    public static String normalizar(String nombre) {
        return nombre == null
                ? ""
                : nombre.strip().replaceAll("(?U)\\s+", " ");
    }

    /** Clave con la que se garantiza que no haya dos Rutas con el mismo nombre. */
    public static String clave(String nombre) {
        return normalizar(nombre).toLowerCase(Locale.ROOT);
    }
}
