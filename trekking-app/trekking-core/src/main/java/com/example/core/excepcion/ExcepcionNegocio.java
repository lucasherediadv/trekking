package com.example.core.excepcion;

/** Base de los errores de negocio: no chequeados, cada presentación decide cómo mostrarlos. */
public abstract class ExcepcionNegocio extends RuntimeException {

    protected ExcepcionNegocio(String mensaje) {
        super(mensaje);
    }
}
