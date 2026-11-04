package com.example.core.salida;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Contrato de persistencia de las Salidas. Lo implementa cada motor de base. */
public interface RepositorioSalidas {

    /** Todas las Salidas, de la más reciente a la más antigua. */
    List<Salida> listar();

    /** Resumen de Participantes y Equipamiento por id de Salida; las Salidas sin nada pueden faltar. */
    Map<Integer, ResumenOperativo> resumenes();

    Optional<Salida> buscarPorId(int id);

    /**
     * Cambia el estado solo si la Salida sigue planificada, de forma atómica. Al dejar de estar
     * planificada libera su Reserva de stock.
     *
     * @return {@code false} si la Salida ya no estaba planificada
     */
    boolean cerrarSiPlanificada(int id, EstadoSalida nuevoEstado);
}
