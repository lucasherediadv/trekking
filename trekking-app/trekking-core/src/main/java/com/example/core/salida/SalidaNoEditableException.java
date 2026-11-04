package com.example.core.salida;

import com.example.core.excepcion.ExcepcionNegocio;

/** Se pidió editar una Salida que ya no está planificada: realizada y cancelada son estados finales. */
public class SalidaNoEditableException extends ExcepcionNegocio {

    public SalidaNoEditableException(EstadoSalida actual) {
        super("Solo se puede editar una Salida planificada: esta ya está " + actual.etiqueta().toLowerCase() + ".");
    }
}
