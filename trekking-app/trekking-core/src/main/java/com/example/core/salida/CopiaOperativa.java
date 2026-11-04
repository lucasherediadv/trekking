package com.example.core.salida;

import java.time.LocalDate;
import java.util.List;

import com.example.core.cliente.Cliente;
import com.example.core.inventario.ElementoInventario;
import com.example.core.itinerario.Checkpoint;
import com.example.core.ruta.Ruta;

/**
 * Datos operativos de una Salida, copiados de los catálogos en el momento de confirmarla (ADR 0001).
 * Quedan aislados de cambios posteriores en la Ruta, los Checkpoints, el inventario y los Clientes.
 */
public record CopiaOperativa(LocalDate fecha, Ruta ruta, List<Checkpoint> checkpoints,
        List<LineaEquipamiento> equipamiento, List<Cliente> participantes) {

    public record LineaEquipamiento(ElementoInventario elemento, int cantidad) {
    }
}
