package com.example.core.salida;

import java.time.LocalDate;

/** Instancia fechada de una Ruta, con los datos de ruta conservados como copia operativa. */
public record Salida(
        int id,
        int rutaId,
        LocalDate fecha,
        EstadoSalida estado,
        String rutaNombre,
        double rutaAltitudMaxima,
        String rutaTipoTerreno,
        String rutaDificultadTecnica,
        String rutaDificultadFisica) {
}
