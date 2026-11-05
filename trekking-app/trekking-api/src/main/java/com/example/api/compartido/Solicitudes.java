package com.example.api.compartido;

import java.time.LocalDate;
import java.util.List;

import com.example.core.excepcion.ErrorCampo;

/** Utilidades comunes de los cuerpos de solicitud de Salida. */
public final class Solicitudes {

    private Solicitudes() {
    }

    public static <T> List<T> orVacia(List<T> lista) {
        return lista == null ? List.of() : lista;
    }

    /** Agrega los errores de formato por fecha o Ruta ausentes. */
    public static void exigirFechaYRuta(LocalDate fecha, Integer rutaId, List<ErrorCampo> errores) {
        if (fecha == null) errores.add(new ErrorCampo("fecha", "La fecha de la Salida es obligatoria."));
        if (rutaId == null) errores.add(new ErrorCampo("rutaId", "La Ruta es obligatoria."));
    }
}
