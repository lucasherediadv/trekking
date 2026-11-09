package com.example.core.inventario;

/** Elemento del catálogo de inventario con su Reserva de stock actual. */
public record ElementoInventario(int id, String nombre, String categoria, int cantidad,
        EstadoElemento estado, int cantidadReservada) {

    public int cantidadDisponible() {
        return cantidad - cantidadReservada;
    }
}
