package com.example.core.inventario;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Contrato de persistencia del Inventario. Lo implementa cada motor de base. */
public interface RepositorioInventario {

    /** Inserta un elemento activo y devuelve su id. */
    int insertar(DatosElemento datos);

    void actualizar(int id, DatosElemento datos);

    void cambiarActivo(int id, boolean activo);

    Optional<ElementoInventario> buscarPorId(int id);

    List<ElementoInventario> listarActivos();

    List<ElementoInventario> listarDadosDeBaja();

    /** Elementos activos cuyo estado no está en {@code estadosExcluidos}. */
    List<ElementoInventario> listarActivosExcluyendo(Set<EstadoElemento> estadosExcluidos);
}
