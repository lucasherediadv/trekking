package com.example.api.ruta;

import com.example.core.ruta.Ruta;

/** Representación JSON de una Ruta. */
public record RutaDto(int id, String nombre, double altitudMaxima, String tipoTerreno,
        String dificultadTecnica, String dificultadFisica, boolean activa, int cantidadCheckpoints,
        boolean borrador) {

    static RutaDto de(Ruta ruta) {
        return new RutaDto(ruta.id(), ruta.nombre(), ruta.altitudMaxima(), ruta.tipoTerreno(),
                ruta.dificultadTecnica(), ruta.dificultadFisica(), ruta.activa(), ruta.cantidadCheckpoints(),
                ruta.esBorrador());
    }
}
