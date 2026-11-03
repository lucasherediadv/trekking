package com.example.desktop.controller;

import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;

public final class ClienteValidator {

    private static final Pattern DNI_VALIDO = Pattern.compile("\\d{7,8}");
    private static final Pattern NOMBRE_VALIDO = Pattern.compile("[\\p{L}]+(?:[ '-][\\p{L}]+)*");
    private static final Pattern EMAIL_VALIDO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern TELEFONO_VALIDO = Pattern.compile("[0-9+() -]{7,20}");

    private ClienteValidator() {
    }

    public static String validarDni(String dni) {
        return dni.isEmpty() || !DNI_VALIDO.matcher(dni).matches()
                ? "El DNI debe contener entre 7 y 8 numeros."
                : null;
    }

    public static String validarNombre(String nombre, String etiqueta) {
        if (nombre.isEmpty()) {
            return etiqueta + " es obligatorio.";
        }
        if (nombre.length() > 50) {
            return etiqueta + " no puede superar los 50 caracteres.";
        }
        return NOMBRE_VALIDO.matcher(nombre).matches()
                ? null
                : etiqueta
                        + " solo puede contener letras, espacios, guiones o apostrofes.";
    }

    public static String validarFechaNacimiento(LocalDate fechaNacimiento) {
        return fechaNacimiento == null
                || fechaNacimiento.isAfter(LocalDate.now())
                        ? "La fecha de nacimiento es obligatoria y no puede ser futura."
                        : null;
    }

    public static String validarEmail(String email) {
        return email.length() > 100
                || (!email.isEmpty() && !EMAIL_VALIDO.matcher(email).matches())
                        ? "Ingresa un email valido de hasta 100 caracteres."
                        : null;
    }

    public static String validarTelefono(String telefono, String etiqueta) {
        if (telefono.isEmpty()) {
            return etiqueta + " es obligatorio.";
        }
        return TELEFONO_VALIDO.matcher(telefono).matches()
                ? null
                : etiqueta + " debe tener un formato valido.";
    }

    public static String validarSexo(String sexo) {
        return sexo == null || sexo.isEmpty()
                ? "Selecciona el sexo del cliente."
                : null;
    }

    public static String validarContacto(
            String valor,
            String nombre,
            String telefono,
            String relacion,
            String etiqueta) {
        boolean iniciado = !nombre.isEmpty()
                || !telefono.isEmpty()
                || !relacion.isEmpty();
        if (!iniciado) {
            return null;
        }
        if (valor.isEmpty()) {
            return "Completa los tres campos del contacto de emergencia.";
        }
        if ("telefono".equals(etiqueta)) {
            return TELEFONO_VALIDO.matcher(valor).matches()
                    ? null
                    : "El telefono del contacto de emergencia no es valido.";
        }
        return null;
    }

    public static boolean esMenor(LocalDate fechaNacimiento) {
        return fechaNacimiento != null
                && Period.between(fechaNacimiento, LocalDate.now()).getYears() < 18;
    }

    public static String validarAutorizacionMenores(
            LocalDate fechaNacimiento,
            boolean autorizacionMenores) {
        return esMenor(fechaNacimiento) && !autorizacionMenores
                ? "Para menores de 18 anos debes marcar la autorizacion de menores."
                : null;
    }

    public static String validarTutor(
            String valor,
            String etiqueta,
            LocalDate fechaNacimiento) {
        if (!esMenor(fechaNacimiento)) {
            return null;
        }
        if (valor.isEmpty()) {
            return "Completa los datos del tutor para un cliente menor de edad.";
        }
        if ("dni".equals(etiqueta)) {
            return validarDni(valor);
        }
        if ("telefono".equals(etiqueta)) {
            return validarTelefono(valor, "El telefono del tutor");
        }
        return NOMBRE_VALIDO.matcher(valor).matches()
                ? null
                : "El nombre y apellido del tutor no son validos.";
    }

    public static String validar(String dni, String nombre, String apellido, LocalDate fechaNacimiento,
            String email, String telefono, String sexo, String contactoNombre, String contactoTelefono,
            String contactoRelacion) {
        return validar(dni, nombre, apellido, fechaNacimiento, email, telefono, sexo, contactoNombre,
            contactoTelefono, contactoRelacion, false, "", "", "", "");
        }

        public static String validar(String dni, String nombre, String apellido, LocalDate fechaNacimiento,
            String email, String telefono, String sexo, String contactoNombre, String contactoTelefono,
            String contactoRelacion, boolean autorizacionMenores, String tutorNombre, String tutorApellido,
            String tutorDni, String tutorTelefono) {
        if (dni.isEmpty() || !DNI_VALIDO.matcher(dni).matches()) {
            return "El DNI es obligatorio y debe contener entre 7 y 8 numeros.";
        }
        if (nombre.isEmpty() || apellido.isEmpty()) {
            return "El nombre y apellido son obligatorios.";
        }
        if (!NOMBRE_VALIDO.matcher(nombre).matches() || !NOMBRE_VALIDO.matcher(apellido).matches()) {
            return "El nombre y apellido solo pueden contener letras, espacios, guiones o apostrofes.";
        }
        if (nombre.length() > 50 || apellido.length() > 50) {
            return "El nombre y apellido no pueden superar los 50 caracteres.";
        }
        if (fechaNacimiento == null || fechaNacimiento.isAfter(LocalDate.now())) {
            return "La fecha de nacimiento es obligatoria y no puede ser futura.";
        }
        if (email.length() > 100 || (!email.isEmpty() && !EMAIL_VALIDO.matcher(email).matches())) {
            return "Ingresa un email valido de hasta 100 caracteres.";
        }
        if (telefono.isEmpty() || !TELEFONO_VALIDO.matcher(telefono).matches()) {
            return "El telefono es obligatorio y debe tener un formato valido.";
        }
        if (sexo == null || sexo.isEmpty()) {
            return "Selecciona el sexo del cliente.";
        }
        boolean contactoIncompleto = contactoNombre.isEmpty() || contactoTelefono.isEmpty()
                || contactoRelacion.isEmpty();
        boolean contactoIniciado = !contactoNombre.isEmpty() || !contactoTelefono.isEmpty()
                || !contactoRelacion.isEmpty();
        if (contactoIniciado && contactoIncompleto) {
            return "Completa nombre, telefono y relacion del contacto de emergencia.";
        }
        if (!contactoTelefono.isEmpty() && !TELEFONO_VALIDO.matcher(contactoTelefono).matches()) {
            return "El telefono del contacto de emergencia no es valido.";
        }
        boolean menor = Period.between(fechaNacimiento, LocalDate.now()).getYears() < 18;
        if (menor && !autorizacionMenores) {
            return "Para menores de 18 anos debes marcar la autorizacion de menores.";
        }
        if (menor && (tutorNombre.isEmpty() || tutorApellido.isEmpty() || tutorDni.isEmpty()
                || tutorTelefono.isEmpty())) {
            return "Para menores de 18 anos debes completar los datos del tutor.";
        }
        if (menor && (!NOMBRE_VALIDO.matcher(tutorNombre).matches()
                || !NOMBRE_VALIDO.matcher(tutorApellido).matches())) {
            return "El nombre y apellido del tutor no son validos.";
        }
        if (menor && !DNI_VALIDO.matcher(tutorDni).matches()) {
            return "El DNI del tutor debe contener entre 7 y 8 numeros.";
        }
        if (menor && !TELEFONO_VALIDO.matcher(tutorTelefono).matches()) {
            return "El telefono del tutor no tiene un formato valido.";
        }
        return null;
    }
}