package com.example.core.cliente;

import java.time.LocalDate;

/** Cliente registrado. Los datos opcionales ausentes son {@code null}. */
public record Cliente(int id, String dni, String nombre, String apellido, LocalDate fechaNacimiento,
        String email, String telefono, String sexo, String contactoEmergenciaNombre,
        String contactoEmergenciaTelefono, String contactoEmergenciaRelacion, boolean autorizacionMenores,
        String tutorNombre, String tutorApellido, String tutorDni, String tutorTelefono, boolean activo,
        LocalDate fechaAlta, LocalDate fechaModificacion) {
}
