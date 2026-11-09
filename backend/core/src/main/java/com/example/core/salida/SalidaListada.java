package com.example.core.salida;

/** Fila del listado de Salidas: la Salida, su resumen operativo y si está vencida a hoy. */
public record SalidaListada(Salida salida, ResumenOperativo resumen, boolean vencida) {
}
