package com.example.api.compartido;

import com.example.core.salida.DatosSalida.LineaEquipamiento;

/** Línea de equipamiento de una solicitud de Salida: elemento y cantidad. */
public record LineaEquipamientoDto(int elementoId, int cantidad) {

    public LineaEquipamiento aLinea() {
        return new LineaEquipamiento(elementoId, cantidad);
    }
}
