package com.example.core.cliente;

import java.util.Objects;
import java.util.Optional;

import com.example.core.excepcion.EntidadNoEncontradaException;

/** Casos de uso de la ficha médica de un Cliente. */
public class CasosDeUsoFichasMedicas {

    private final RepositorioClientes clientes;
    private final AlmacenFichasMedicas almacen;

    public CasosDeUsoFichasMedicas(RepositorioClientes clientes, AlmacenFichasMedicas almacen) {
        this.clientes = Objects.requireNonNull(clientes, "clientes");
        this.almacen = Objects.requireNonNull(almacen, "almacen");
    }

    /** Adjunta la ficha del Cliente; si ya tenía una, la reemplaza. */
    public void adjuntar(int clienteId, String nombreArchivo, byte[] contenido) {
        verificarCliente(clienteId);
        almacen.guardar(clienteId, new FichaMedica(nombreArchivo, contenido));
    }

    public Optional<FichaMedica> consultar(int clienteId) {
        verificarCliente(clienteId);
        return almacen.buscar(clienteId);
    }

    public void quitar(int clienteId) {
        verificarCliente(clienteId);
        almacen.quitar(clienteId);
    }

    private void verificarCliente(int clienteId) {
        if (clientes.buscarPorId(clienteId).isEmpty()) throw new EntidadNoEncontradaException("Cliente", clienteId);
    }
}
