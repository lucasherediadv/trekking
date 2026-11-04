package com.example.core.itinerario;

/** Punto de control de una Ruta. Un {@code id} igual a 0 indica que todavía no fue guardado. */
public record Checkpoint(long id, String nombre, String hora, double latitud, double longitud,
        double altitud, String descripcion) {

    public boolean esNuevo() {
        return id == 0;
    }

    public Checkpoint conId(long nuevoId) {
        return new Checkpoint(nuevoId, nombre, hora, latitud, longitud, altitud, descripcion);
    }
}
