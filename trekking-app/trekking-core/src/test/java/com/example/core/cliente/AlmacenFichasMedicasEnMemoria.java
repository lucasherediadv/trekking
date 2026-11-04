package com.example.core.cliente;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Almacén falso para probar los casos de uso sin base de datos. */
class AlmacenFichasMedicasEnMemoria implements AlmacenFichasMedicas {

    private final Map<Integer, FichaMedica> fichas = new HashMap<>();

    @Override
    public void guardar(int clienteId, FichaMedica ficha) {
        fichas.put(clienteId, ficha);
    }

    @Override
    public Optional<FichaMedica> buscar(int clienteId) {
        return Optional.ofNullable(fichas.get(clienteId));
    }

    @Override
    public void quitar(int clienteId) {
        fichas.remove(clienteId);
    }
}
