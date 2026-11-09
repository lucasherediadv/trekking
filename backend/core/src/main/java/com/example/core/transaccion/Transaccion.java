package com.example.core.transaccion;

import java.util.function.Supplier;

/**
 * Contrato de transacción: ejecuta un bloque de trabajo de forma atómica y serializada respecto de
 * cualquier otra transacción, aunque provenga de otra instancia de la aplicación (ADR 0003, ADR 0005).
 * Si el bloque lanza una excepción, ninguno de sus cambios queda guardado.
 */
public interface Transaccion {

    <T> T ejecutar(Supplier<T> trabajo);
}
