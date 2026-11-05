package com.example.api.ruta;

import com.example.core.ruta.DatosRuta;

/** Cuerpo de la petición para crear o editar una Ruta. Un campo ausente llega como null y core lo valida. */
public record DatosRutaDto(String nombre, Double altitudMaxima, String tipoTerreno,
        String dificultadTecnica, String dificultadFisica) {

    DatosRuta aDatos() {
        double altitud = altitudMaxima == null ? Double.NaN : altitudMaxima;
        return new DatosRuta(nombre, altitud, tipoTerreno, dificultadTecnica, dificultadFisica);
    }
}
