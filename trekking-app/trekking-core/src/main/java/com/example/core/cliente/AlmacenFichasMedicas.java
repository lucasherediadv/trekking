package com.example.core.cliente;

import java.util.Optional;

/** Contrato de almacenamiento de las fichas médicas. Core no sabe dónde ni cómo se guardan. */
public interface AlmacenFichasMedicas {

    /** Guarda la ficha del Cliente, reemplazando la anterior si la había. */
    void guardar(int clienteId, FichaMedica ficha);

    Optional<FichaMedica> buscar(int clienteId);

    /** Elimina la ficha del Cliente; no hace nada si no tenía. */
    void quitar(int clienteId);
}
