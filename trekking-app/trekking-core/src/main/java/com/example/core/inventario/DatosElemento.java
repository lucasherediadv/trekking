package com.example.core.inventario;

/** Datos editables de un elemento de inventario. */
public record DatosElemento(String nombre, String categoria, int cantidad, EstadoElemento estado) {
}
