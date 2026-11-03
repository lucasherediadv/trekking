package com.example.desktop.model;

/** Equipamiento planned for a Salida; new elements remain transient until confirmation. */
public record AsignacionEquipamiento(Equipamiento equipamiento, int cantidad, boolean nuevo) {
}
