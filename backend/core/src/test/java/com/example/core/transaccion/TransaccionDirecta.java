package com.example.core.transaccion;

import java.util.function.Supplier;

/** Transacción falsa para tests de casos de uso: ejecuta el trabajo sin aislamiento ni reversión. */
public class TransaccionDirecta implements Transaccion {

    @Override
    public <T> T ejecutar(Supplier<T> trabajo) {
        return trabajo.get();
    }
}
