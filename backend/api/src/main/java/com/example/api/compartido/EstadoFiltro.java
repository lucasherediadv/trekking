package com.example.api.compartido;

import java.util.List;
import java.util.function.Supplier;

/** Valores del parámetro {@code estado} de los listados con Baja (Rutas, Inventario, Clientes). */
public enum EstadoFiltro {
    activas, bajas, todas;

    /** Elige el listado que corresponde al estado; solo se consulta la fuente elegida. */
    public <T> List<T> seleccionar(Supplier<List<T>> activas, Supplier<List<T>> bajas, Supplier<List<T>> todas) {
        return switch (this) {
            case activas -> activas.get();
            case bajas -> bajas.get();
            case todas -> todas.get();
        };
    }
}
