package com.example.core.salida;

/** Texto resumido de los Participantes y del Equipamiento de una Salida, para listar y buscar. */
public record ResumenOperativo(String participantes, String equipamiento) {

    public static final ResumenOperativo VACIO = new ResumenOperativo("", "");
}
