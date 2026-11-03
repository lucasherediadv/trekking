package com.example.desktop.model;

import java.time.LocalDate;

public class Cliente {

    private final int id;
    private final String dni;
    private final String nombre;
    private final String apellido;
    private final LocalDate fechaNacimiento;
    private final String email;
    private final String telefono;
    private final String sexo;
    private final String contactoEmergenciaNombre;
    private final String contactoEmergenciaTelefono;
    private final String contactoEmergenciaRelacion;
    private final boolean autorizacionMenores;
    private final String tutorNombre;
    private final String tutorApellido;
    private final String tutorDni;
    private final String tutorTelefono;
    private final boolean activo;
    private final LocalDate fechaAlta;
    private final LocalDate fechaModificacion;

    public Cliente(
            int id,
            String dni,
            String nombre,
            String apellido,
            LocalDate fechaNacimiento,
            String email,
            String telefono,
            String sexo,
            String contactoEmergenciaNombre,
            String contactoEmergenciaTelefono,
            String contactoEmergenciaRelacion,
            boolean autorizacionMenores,
            String tutorNombre,
            String tutorApellido,
            String tutorDni,
            String tutorTelefono,
            boolean activo,
            LocalDate fechaAlta,
            LocalDate fechaModificacion) {

        this.id = id;
        this.dni = dni;
        this.nombre = nombre;
        this.apellido = apellido;
        this.fechaNacimiento = fechaNacimiento;
        this.email = email;
        this.telefono = telefono;
        this.sexo = sexo;
        this.contactoEmergenciaNombre = contactoEmergenciaNombre;
        this.contactoEmergenciaTelefono = contactoEmergenciaTelefono;
        this.contactoEmergenciaRelacion = contactoEmergenciaRelacion;
        this.autorizacionMenores = autorizacionMenores;
        this.tutorNombre = tutorNombre;
        this.tutorApellido = tutorApellido;
        this.tutorDni = tutorDni;
        this.tutorTelefono = tutorTelefono;
        this.activo = activo;
        this.fechaAlta = fechaAlta;
        this.fechaModificacion = fechaModificacion;
    }

    public int getId() {
        return id;
    }

    public String getDni() {
        return dni;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getSexo() {
        return sexo;
    }

    public String getContactoEmergenciaNombre() {
        return contactoEmergenciaNombre;
    }

    public String getContactoEmergenciaTelefono() {
        return contactoEmergenciaTelefono;
    }

    public String getContactoEmergenciaRelacion() {
        return contactoEmergenciaRelacion;
    }

    public boolean isAutorizacionMenores() {
        return autorizacionMenores;
    }

    public String getTutorNombre() {
        return tutorNombre;
    }

    public String getTutorApellido() {
        return tutorApellido;
    }

    public String getTutorDni() {
        return tutorDni;
    }

    public String getTutorTelefono() {
        return tutorTelefono;
    }

    public boolean isActivo() {
        return activo;
    }

    public LocalDate getFechaAlta() {
        return fechaAlta;
    }

    public LocalDate getFechaModificacion() {
        return fechaModificacion;
    }
}

