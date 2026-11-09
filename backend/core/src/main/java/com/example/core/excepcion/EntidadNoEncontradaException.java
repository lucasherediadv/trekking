package com.example.core.excepcion;

/** La entidad pedida no existe (por ejemplo, fue eliminada por otra instancia). */
public class EntidadNoEncontradaException extends ExcepcionNegocio {

    public EntidadNoEncontradaException(String entidad, int id) {
        super(entidad + " con id " + id + " no existe.");
    }
}
