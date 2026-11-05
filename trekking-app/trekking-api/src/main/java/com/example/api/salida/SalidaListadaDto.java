package com.example.api.salida;

import java.time.LocalDate;

import com.example.core.salida.EstadoSalida;
import com.example.core.salida.SalidaListada;

/** Fila del listado de Salidas: datos básicos, resumen operativo y condición de vencida. */
public record SalidaListadaDto(int id, int rutaId, LocalDate fecha, EstadoSalida estado, String rutaNombre,
        String participantes, String equipamiento, boolean vencida) {

    public static SalidaListadaDto de(SalidaListada l) {
        return new SalidaListadaDto(l.salida().id(), l.salida().rutaId(), l.salida().fecha(), l.salida().estado(),
                l.salida().rutaNombre(), l.resumen().participantes(), l.resumen().equipamiento(), l.vencida());
    }
}
