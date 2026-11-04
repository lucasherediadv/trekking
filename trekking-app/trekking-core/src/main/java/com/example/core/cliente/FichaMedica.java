package com.example.core.cliente;

/** Ficha médica adjunta a un Cliente: el contenido del archivo y su nombre original. */
public record FichaMedica(String nombreArchivo, byte[] contenido) {
}
