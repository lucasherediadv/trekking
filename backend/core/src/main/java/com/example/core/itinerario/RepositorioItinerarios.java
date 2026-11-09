package com.example.core.itinerario;

import java.util.List;

/** Contrato de persistencia de los Itinerarios. Lo implementa cada motor de base. */
public interface RepositorioItinerarios {

    /** Checkpoints de la Ruta en su orden guardado. */
    List<Checkpoint> listar(int rutaId);

    /**
     * Reemplaza de forma atómica el Itinerario de la Ruta por los Checkpoints dados, ya ordenados:
     * los de id 0 se crean, los de id existente se actualizan y los que faltan se eliminan.
     */
    void reemplazar(int rutaId, List<Checkpoint> ordenados);
}
