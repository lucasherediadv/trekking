package com.example.api.itinerario;

import com.example.core.itinerario.Checkpoint;

/**
 * Representación JSON de un Checkpoint, tanto en la lectura como en el cuerpo del PUT. La posición en
 * la lista es el orden explícito; {@code id} se ignora al guardar. Un campo ausente llega como null y
 * core lo valida.
 */
public record CheckpointDto(Long id, String nombre, String hora, Double latitud, Double longitud,
        Double altitud, String descripcion) {

    static CheckpointDto de(Checkpoint cp) {
        return new CheckpointDto(cp.id(), cp.nombre(), cp.hora(), cp.latitud(), cp.longitud(), cp.altitud(),
                cp.descripcion());
    }

    public Checkpoint aCheckpoint() {
        return new Checkpoint(0, nombre, hora, numero(latitud), numero(longitud), numero(altitud), descripcion);
    }

    private static double numero(Double valor) {
        return valor == null ? Double.NaN : valor;
    }
}
