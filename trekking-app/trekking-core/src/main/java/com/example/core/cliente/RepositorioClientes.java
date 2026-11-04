package com.example.core.cliente;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de Clientes. Recibe los datos ya normalizados y las fechas de alta y
 * modificación desde el reloj de core. No gestiona la ficha médica.
 */
public interface RepositorioClientes {

    /** Inserta un Cliente activo y devuelve su id. */
    int insertar(DatosCliente datos, LocalDate hoy);

    void actualizar(int id, DatosCliente datos, LocalDate hoy);

    void cambiarActivo(int id, boolean activo, LocalDate hoy);

    Optional<Cliente> buscarPorId(int id);

    Optional<Cliente> buscarPorDni(String dni);

    /** Todos los Clientes, activos e inactivos, ordenados por apellido y nombre. */
    List<Cliente> listarTodos();
}
