package com.example.core.cliente;

import java.time.LocalDate;

/** Datos editables de un Cliente. Los textos ausentes pueden venir vacíos o nulos. */
public record DatosCliente(String dni, String nombre, String apellido, LocalDate fechaNacimiento, String email,
        String telefono, String sexo, String contactoEmergenciaNombre, String contactoEmergenciaTelefono,
        String contactoEmergenciaRelacion, boolean autorizacionMenores, String tutorNombre,
        String tutorApellido, String tutorDni, String tutorTelefono) {
}
