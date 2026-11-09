package com.example.api.cliente;

import java.time.LocalDate;

import com.example.core.cliente.Cliente;

/** Representación JSON de un Cliente; {@code menor} lo calcula core con su reloj. */
public record ClienteDto(int id, String dni, String nombre, String apellido, LocalDate fechaNacimiento,
        String email, String telefono, String sexo, String contactoEmergenciaNombre,
        String contactoEmergenciaTelefono, String contactoEmergenciaRelacion, boolean autorizacionMenores,
        String tutorNombre, String tutorApellido, String tutorDni, String tutorTelefono, boolean activo,
        LocalDate fechaAlta, LocalDate fechaModificacion, boolean menor) {

    static ClienteDto de(Cliente c, boolean menor) {
        return new ClienteDto(c.id(), c.dni(), c.nombre(), c.apellido(), c.fechaNacimiento(), c.email(),
                c.telefono(), c.sexo(), c.contactoEmergenciaNombre(), c.contactoEmergenciaTelefono(),
                c.contactoEmergenciaRelacion(), c.autorizacionMenores(), c.tutorNombre(), c.tutorApellido(),
                c.tutorDni(), c.tutorTelefono(), c.activo(), c.fechaAlta(), c.fechaModificacion(), menor);
    }
}
