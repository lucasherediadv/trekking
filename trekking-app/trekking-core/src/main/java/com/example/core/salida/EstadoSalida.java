package com.example.core.salida;

/** Estado de una Salida. Realizada y cancelada son finales (ADR 0001). */
public enum EstadoSalida {
    PLANIFICADA("Planificada"),
    REALIZADA("Realizada"),
    CANCELADA("Cancelada");

    private final String etiqueta;

    EstadoSalida(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Texto para mostrar al usuario. */
    public String etiqueta() {
        return etiqueta;
    }
}
