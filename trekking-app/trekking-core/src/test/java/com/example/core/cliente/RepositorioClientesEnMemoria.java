package com.example.core.cliente;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Repositorio falso para probar los casos de uso sin base de datos. */
public class RepositorioClientesEnMemoria implements RepositorioClientes {

    private final Map<Integer, Cliente> clientes = new LinkedHashMap<>();
    private int siguienteId = 1;

    @Override
    public int insertar(DatosCliente datos, LocalDate hoy) {
        int id = siguienteId++;
        clientes.put(id, conId(id, datos, true, hoy, hoy));
        return id;
    }

    @Override
    public void actualizar(int id, DatosCliente datos, LocalDate hoy) {
        Cliente actual = clientes.get(id);
        clientes.put(id, conId(id, datos, actual.activo(), actual.fechaAlta(), hoy));
    }

    @Override
    public void cambiarActivo(int id, boolean activo, LocalDate hoy) {
        Cliente c = clientes.get(id);
        clientes.put(id, new Cliente(id, c.dni(), c.nombre(), c.apellido(), c.fechaNacimiento(), c.email(),
                c.telefono(), c.sexo(), c.contactoEmergenciaNombre(), c.contactoEmergenciaTelefono(),
                c.contactoEmergenciaRelacion(), c.autorizacionMenores(), c.tutorNombre(), c.tutorApellido(),
                c.tutorDni(), c.tutorTelefono(), activo, c.fechaAlta(), hoy));
    }

    @Override
    public Optional<Cliente> buscarPorId(int id) {
        return Optional.ofNullable(clientes.get(id));
    }

    @Override
    public Optional<Cliente> buscarPorDni(String dni) {
        return clientes.values().stream().filter(c -> c.dni().equals(dni)).findFirst();
    }

    @Override
    public List<Cliente> listarTodos() {
        List<Cliente> lista = new ArrayList<>(clientes.values());
        lista.sort(Comparator.comparing(Cliente::apellido).thenComparing(Cliente::nombre));
        return lista;
    }

    private static Cliente conId(int id, DatosCliente d, boolean activo, LocalDate alta, LocalDate modificacion) {
        return new Cliente(id, d.dni(), d.nombre(), d.apellido(), d.fechaNacimiento(), d.email(), d.telefono(),
                d.sexo(), d.contactoEmergenciaNombre(), d.contactoEmergenciaTelefono(),
                d.contactoEmergenciaRelacion(), d.autorizacionMenores(), d.tutorNombre(), d.tutorApellido(),
                d.tutorDni(), d.tutorTelefono(), activo, alta, modificacion);
    }
}
