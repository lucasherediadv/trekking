package com.example.core.itinerario;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.core.ruta.RepositorioRutasEnMemoria;

/** Repositorio falso para tests de casos de uso; mantiene al día la cantidad de Checkpoints de la Ruta. */
public class RepositorioItinerariosEnMemoria implements RepositorioItinerarios {

    private final RepositorioRutasEnMemoria rutas;
    private final Map<Integer, List<Checkpoint>> guardados = new HashMap<>();
    private long siguienteId = 1;

    public RepositorioItinerariosEnMemoria(RepositorioRutasEnMemoria rutas) {
        this.rutas = rutas;
    }

    @Override
    public List<Checkpoint> listar(int rutaId) {
        return guardados.getOrDefault(rutaId, List.of());
    }

    @Override
    public void reemplazar(int rutaId, List<Checkpoint> ordenados) {
        guardados.put(rutaId, ordenados.stream().map(cp -> cp.esNuevo() ? cp.conId(siguienteId++) : cp).toList());
        rutas.fijarCheckpoints(rutaId, ordenados.size());
    }
}
