package com.example.desktop.model;

/** Participante elegido o preparado para persistirse al confirmar una Salida. */
public record AsignacionParticipante(Cliente cliente, boolean nuevo, byte[] fichaMedica, String nombreFichaMedica) {
    public AsignacionParticipante(Cliente cliente, boolean nuevo) {
        this(cliente, nuevo, null, null);
    }
}
