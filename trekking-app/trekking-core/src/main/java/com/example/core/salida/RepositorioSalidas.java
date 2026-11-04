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

    /**
     * Guarda una Salida planificada con su Copia operativa y devuelve su id. No valida: lo hace el
     * caso de uso dentro de la misma transacción.
     */
    int guardar(CopiaOperativa copia);

    /**
     * Copia operativa vigente de la Salida. Cada elemento y participante conserva los datos copiados
     * al confirmar; la cantidad total, el estado y la Reserva del elemento son los actuales del catálogo.
     */
    Optional<CopiaOperativa> buscarCopia(int id);

    /**
     * Reemplaza la Copia operativa de la Salida (fecha, datos de Ruta, Checkpoints, Equipamiento y
     * Participantes) sin conservar la anterior (ADR 0001). No valida: lo hace el caso de uso dentro
     * de la misma transacción.
     */
    void reemplazarCopia(int id, CopiaOperativa copia);
}
