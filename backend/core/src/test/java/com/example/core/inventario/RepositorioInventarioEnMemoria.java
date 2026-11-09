package com.example.core.inventario;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/** Repositorio falso para tests de casos de uso. */
public class RepositorioInventarioEnMemoria implements RepositorioInventario {

    private record Fila(DatosElemento datos, boolean activo, int reservada) {
    }

    private final Map<Integer, Fila> filas = new TreeMap<>();
    private int siguienteId = 1;

    /** Simula la Reserva de stock que generan las Salidas planificadas. */
    public void reservar(int id, int cantidad) {
        Fila f = filas.get(id);
        filas.put(id, new Fila(f.datos, f.activo, cantidad));
    }

    @Override
    public int insertar(DatosElemento datos) {
        int id = siguienteId++;
        filas.put(id, new Fila(datos, true, 0));
        return id;
    }

    @Override
    public void actualizar(int id, DatosElemento datos) {
        Fila f = filas.get(id);
        filas.put(id, new Fila(datos, f.activo, f.reservada));
    }

    @Override
    public void cambiarActivo(int id, boolean activo) {
        Fila f = filas.get(id);
        filas.put(id, new Fila(f.datos, activo, f.reservada));
    }

    @Override
    public Optional<ElementoInventario> buscarPorId(int id) {
        return Optional.ofNullable(filas.get(id)).map(f -> aElemento(id, f));
    }

    @Override
    public List<ElementoInventario> listarActivos() {
        return listar(true, Set.of());
    }

    @Override
    public List<ElementoInventario> listarDadosDeBaja() {
        return listar(false, Set.of());
    }

    @Override
    public List<ElementoInventario> listarActivosExcluyendo(Set<EstadoElemento> estadosExcluidos) {
        return listar(true, estadosExcluidos);
    }

    private List<ElementoInventario> listar(boolean activo, Set<EstadoElemento> excluidos) {
        List<ElementoInventario> resultado = new ArrayList<>();
        filas.forEach((id, f) -> {
            if (f.activo == activo && !excluidos.contains(f.datos.estado())) resultado.add(aElemento(id, f));
        });
        return resultado;
    }

    private static ElementoInventario aElemento(int id, Fila f) {
        DatosElemento d = f.datos;
        return new ElementoInventario(id, d.nombre(), d.categoria(), d.cantidad(), d.estado(), f.reservada);
    }
}
