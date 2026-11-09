package com.example.api.dashboard;

import com.example.core.dashboard.CasosDeUsoDashboard.ProximaSalida;

/** Salida próxima: fecha ISO-8601 y horarios {@code HH:mm} (nulos si la Salida no tiene Checkpoints). */
public record ProximaSalidaDto(int id, String nombreRuta, String fecha, String horaInicio, String horaFin,
        int participantes) {

    static ProximaSalidaDto de(ProximaSalida salida) {
        return new ProximaSalidaDto(salida.id(), salida.nombreRuta(), salida.fecha().toString(),
                salida.horaInicio(), salida.horaFin(), salida.participantes());
    }
}
