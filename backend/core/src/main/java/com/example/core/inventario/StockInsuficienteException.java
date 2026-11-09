package com.example.core.inventario;

import com.example.core.excepcion.ExcepcionNegocio;

/** Se pidió reservar más cantidad de un elemento de la que hay disponible. */
public class StockInsuficienteException extends ExcepcionNegocio {

    private final String elemento;
    private final int disponible;

    public StockInsuficienteException(String elemento, int disponible) {
        super("No hay cantidad suficiente de " + elemento + ". Disponible: " + disponible + ".");
        this.elemento = elemento;
        this.disponible = disponible;
    }

    /** Nombre del elemento sin stock suficiente. */
    public String elemento() {
        return elemento;
    }

    public int disponible() {
        return disponible;
    }
}
