package com.example.api.salida;

import java.time.LocalDate;

import com.example.core.salida.EstadoSalida;
import com.example.core.salida.Salida;

/** Representación JSON de una Salida (detalle). Reutilizable por los endpoints de escritura. */
public record SalidaDto(int id, int rutaId, LocalDate fecha, EstadoSalida estado, String rutaNombre,
        double rutaAltitudMaxima, String rutaTipoTerreno, String rutaDificultadTecnica,
        String rutaDificultadFisica) {

    public static SalidaDto de(Salida s) {
        return new SalidaDto(s.id(), s.rutaId(), s.fecha(), s.estado(), s.rutaNombre(), s.rutaAltitudMaxima(),
                s.rutaTipoTerreno(), s.rutaDificultadTecnica(), s.rutaDificultadFisica());
    }
}
