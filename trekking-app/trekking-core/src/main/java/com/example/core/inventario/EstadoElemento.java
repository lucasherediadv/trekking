package com.example.core.inventario;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Condición física de un elemento de inventario. Estar asignado no es un estado: se deduce de la
 * Reserva de stock. Esta es la única definición de qué estados impiden asignar a una Salida.
 */
public enum EstadoElemento {
    DISPONIBLE("Disponible", false),
    EN_REPARACION("En Reparación", true),
    EN_INSPECCION("En Inspección", false),
    FUERA_DE_SERVICIO("Fuera de Servicio", true),
    EXTRAVIADO("Extraviado", true);

    private final String etiqueta;
    private final boolean impideAsignar;

    EstadoElemento(String etiqueta, boolean impideAsignar) {
        this.etiqueta = etiqueta;
        this.impideAsignar = impideAsignar;
    }

    /** Texto para mostrar al usuario. */
    public String etiqueta() {
        return etiqueta;
    }

    public boolean impideAsignar() {
        return impideAsignar;
    }

    public static Set<EstadoElemento> noAsignables() {
        Set<EstadoElemento> estados = EnumSet.noneOf(EstadoElemento.class);
        for (EstadoElemento estado : values()) {
            if (estado.impideAsignar) estados.add(estado);
        }
        return estados;
    }

    public static Optional<EstadoElemento> desdeEtiqueta(String etiqueta) {
        return Arrays.stream(values()).filter(e -> e.etiqueta.equals(etiqueta)).findFirst();
    }
}
