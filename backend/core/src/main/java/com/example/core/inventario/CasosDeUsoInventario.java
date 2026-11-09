package com.example.core.inventario;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.example.core.excepcion.EntidadNoEncontradaException;
import com.example.core.excepcion.ErrorCampo;
import com.example.core.excepcion.ValidacionException;

/** Casos de uso del Inventario: única puerta de entrada para cualquier presentación. */
public class CasosDeUsoInventario {

    private final RepositorioInventario repositorio;

    public CasosDeUsoInventario(RepositorioInventario repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
    }

    public ElementoInventario alta(DatosElemento datos) {
        validar(datos);
        int id = repositorio.insertar(datos);
        return repositorio.buscarPorId(id).orElseThrow();
    }

    public ElementoInventario editar(int id, DatosElemento datos) {
        ElementoInventario actual = obtener(id);
        List<ErrorCampo> errores = new ArrayList<>(ValidadorElemento.validar(datos));
        if (datos.cantidad() < actual.cantidadReservada() && errores.stream().noneMatch(e -> e.campo().equals("cantidad"))) {
            errores.add(new ErrorCampo("cantidad", "La cantidad total no puede ser menor que la cantidad reservada ("
                    + actual.cantidadReservada() + ")."));
        }
        if (!errores.isEmpty()) throw new ValidacionException(errores);
        repositorio.actualizar(id, datos);
        return obtener(id);
    }

    public void darDeBaja(int id) {
        obtener(id);
        repositorio.cambiarActivo(id, false);
    }

    public void reactivar(int id) {
        obtener(id);
        repositorio.cambiarActivo(id, true);
    }

    public ElementoInventario obtener(int id) {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Elemento de inventario", id));
    }

    public List<ElementoInventario> listarActivos() {
        return repositorio.listarActivos();
    }

    public List<ElementoInventario> listarDadosDeBaja() {
        return repositorio.listarDadosDeBaja();
    }

    /** Elementos activos que pueden asignarse a una Salida según su estado. */
    public List<ElementoInventario> listarAsignables() {
        return repositorio.listarActivosExcluyendo(EstadoElemento.noAsignables());
    }

    private static void validar(DatosElemento datos) {
        List<ErrorCampo> errores = ValidadorElemento.validar(datos);
        if (!errores.isEmpty()) throw new ValidacionException(errores);
    }
}
