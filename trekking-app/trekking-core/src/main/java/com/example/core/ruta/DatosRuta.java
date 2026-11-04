package com.example.core.ruta;

/** Datos editables de una Ruta. */
public record DatosRuta(String nombre, double altitudMaxima, String tipoTerreno,
        String dificultadTecnica, String dificultadFisica) {
}
