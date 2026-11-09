package com.example.core.salida;

import com.example.core.excepcion.ExcepcionNegocio;

/** Se pidió cerrar una Salida que ya no está planificada: realizada y cancelada son estados finales. */
public class TransicionInvalidaException extends ExcepcionNegocio {

    public TransicionInvalidaException(EstadoSalida actual) {
        super("Solo se puede cerrar una Salida planificada: esta ya está " + actual.etiqueta().toLowerCase() + ".");
    }
}
