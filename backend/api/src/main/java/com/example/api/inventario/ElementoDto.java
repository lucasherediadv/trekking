package com.example.api.inventario;

import com.example.core.inventario.ElementoInventario;

/** Representación JSON de un elemento de Inventario. {@code activo} es la vigencia (no dado de baja). */
public record ElementoDto(int id, String nombre, String categoria, int cantidad, int cantidadReservada,
        int cantidadDisponible, String estado, String estadoEtiqueta, boolean activo, boolean asignable) {

    static ElementoDto de(ElementoInventario elemento, boolean activo) {
        return new ElementoDto(elemento.id(), elemento.nombre(), elemento.categoria(), elemento.cantidad(),
                elemento.cantidadReservada(), elemento.cantidadDisponible(), elemento.estado().name(),
                elemento.estado().etiqueta(), activo, activo && !elemento.estado().impideAsignar());
    }
}
