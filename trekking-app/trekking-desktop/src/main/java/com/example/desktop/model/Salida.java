package com.example.desktop.model;

import java.time.LocalDate;

public record Salida(
        int id,
        int rutaId,
        LocalDate fecha,
        String estado,
        String rutaNombre,
        double rutaAltitudMaxima,
        String rutaTipoTerreno,
        String rutaDificultadTecnica,
        String rutaDificultadFisica) {
}
