package com.example.core.itinerario;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Secuencia ordenada de Checkpoints de una Ruta (ADR 0002): se ordena por hora y, ante horas iguales,
 * conserva la posición explícita con que se la recibió. Es inmutable: cada cambio devuelve otro Itinerario.
 */
public final class Itinerario {

    private final List<Checkpoint> checkpoints;

    private Itinerario(List<Checkpoint> checkpoints) {
        this.checkpoints = checkpoints;
    }

    /** Ordena por hora; el orden de la lista recibida desempata las horas iguales. */
    public static Itinerario de(List<Checkpoint> checkpoints) {
        List<Checkpoint> ordenados = new ArrayList<>(checkpoints);
        ordenados.sort(Comparator.comparing(Checkpoint::hora)); // estable
        return new Itinerario(List.copyOf(ordenados));
    }

    public static Itinerario vacio() {
        return new Itinerario(List.of());
    }

    public List<Checkpoint> checkpoints() {
        return checkpoints;
    }

    public int cantidad() {
        return checkpoints.size();
    }

    /** El nuevo Checkpoint queda al final de los que comparten su hora. */
    public Itinerario agregar(Checkpoint nuevo) {
        List<Checkpoint> lista = new ArrayList<>(checkpoints);
        lista.add(nuevo);
        return de(lista);
    }

    /** Reemplaza el Checkpoint de la posición indicada; conserva su lugar entre los de igual hora. */
    public Itinerario reemplazar(int posicion, Checkpoint actualizado) {
        verificar(posicion);
        List<Checkpoint> lista = new ArrayList<>(checkpoints);
        lista.set(posicion, actualizado);
        return de(lista);
    }

    public Itinerario quitar(int posicion) {
        verificar(posicion);
        List<Checkpoint> lista = new ArrayList<>(checkpoints);
        lista.remove(posicion);
        return de(lista);
    }

    private void verificar(int posicion) {
        if (posicion < 0 || posicion >= checkpoints.size()) {
            throw new IndexOutOfBoundsException("Posición de Checkpoint inexistente: " + posicion);
        }
    }
}
