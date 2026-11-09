package com.example.core.cliente;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.example.core.excepcion.ErrorCampo;

/** Reglas de validación de un Cliente. No tiene efectos: solo devuelve errores por campo. */
public final class ValidadorCliente {

    public static final int MAYORIA_DE_EDAD = 18;

    private static final Pattern DNI = Pattern.compile("\\d{7,8}");
    private static final Pattern NOMBRE = Pattern.compile("[\\p{L}]+(?:[ '-][\\p{L}]+)*");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern TELEFONO = Pattern.compile("[0-9+() -]{7,20}");

    private ValidadorCliente() {
    }

    public static boolean esMenor(LocalDate fechaNacimiento, LocalDate hoy) {
        return fechaNacimiento != null && Period.between(fechaNacimiento, hoy).getYears() < MAYORIA_DE_EDAD;
    }

    /** Valida todos los campos; {@code hoy} es la fecha del reloj de core. */
    public static List<ErrorCampo> validar(DatosCliente datos, LocalDate hoy) {
        List<ErrorCampo> errores = new ArrayList<>();
        String dni = texto(datos.dni());
        if (!DNI.matcher(dni).matches()) {
            errores.add(new ErrorCampo("dni", "El DNI debe contener entre 7 y 8 numeros."));
        }
        agregar(errores, "nombre", errorNombre(texto(datos.nombre()), "El nombre"));
        agregar(errores, "apellido", errorNombre(texto(datos.apellido()), "El apellido"));
        LocalDate nacimiento = datos.fechaNacimiento();
        if (nacimiento == null || nacimiento.isAfter(hoy)) {
            errores.add(new ErrorCampo("fechaNacimiento",
                    "La fecha de nacimiento es obligatoria y no puede ser futura."));
        }
        String email = texto(datos.email());
        if (email.length() > 100 || (!email.isEmpty() && !EMAIL.matcher(email).matches())) {
            errores.add(new ErrorCampo("email", "Ingresa un email valido de hasta 100 caracteres."));
        }
        agregar(errores, "telefono", errorTelefono(texto(datos.telefono()), "El telefono"));
        if (texto(datos.sexo()).isEmpty()) {
            errores.add(new ErrorCampo("sexo", "Selecciona el sexo del cliente."));
        }
        validarContacto(errores, datos);
        validarMenor(errores, datos, nacimiento != null && !nacimiento.isAfter(hoy) && esMenor(nacimiento, hoy));
        return errores;
    }

    private static void validarContacto(List<ErrorCampo> errores, DatosCliente datos) {
        String nombre = texto(datos.contactoEmergenciaNombre());
        String telefono = texto(datos.contactoEmergenciaTelefono());
        String relacion = texto(datos.contactoEmergenciaRelacion());
        if (nombre.isEmpty() && telefono.isEmpty() && relacion.isEmpty()) return;
        String incompleto = "Completa los tres campos del contacto de emergencia.";
        if (nombre.isEmpty()) errores.add(new ErrorCampo("contactoEmergenciaNombre", incompleto));
        if (telefono.isEmpty()) {
            errores.add(new ErrorCampo("contactoEmergenciaTelefono", incompleto));
        } else if (!TELEFONO.matcher(telefono).matches()) {
            errores.add(new ErrorCampo("contactoEmergenciaTelefono",
                    "El telefono del contacto de emergencia no es valido."));
        }
        if (relacion.isEmpty()) errores.add(new ErrorCampo("contactoEmergenciaRelacion", incompleto));
    }

    private static void validarMenor(List<ErrorCampo> errores, DatosCliente datos, boolean menor) {
        if (!menor) return;
        if (!datos.autorizacionMenores()) {
            errores.add(new ErrorCampo("autorizacionMenores",
                    "Para menores de 18 anos debes marcar la autorizacion de menores."));
        }
        String vacio = "Completa los datos del tutor para un cliente menor de edad.";
        String tutorNombre = texto(datos.tutorNombre());
        String tutorApellido = texto(datos.tutorApellido());
        String tutorDni = texto(datos.tutorDni());
        String tutorTelefono = texto(datos.tutorTelefono());
        String nombreInvalido = "El nombre y apellido del tutor no son validos.";
        if (tutorNombre.isEmpty()) errores.add(new ErrorCampo("tutorNombre", vacio));
        else if (!NOMBRE.matcher(tutorNombre).matches()) errores.add(new ErrorCampo("tutorNombre", nombreInvalido));
        if (tutorApellido.isEmpty()) errores.add(new ErrorCampo("tutorApellido", vacio));
        else if (!NOMBRE.matcher(tutorApellido).matches()) errores.add(new ErrorCampo("tutorApellido", nombreInvalido));
        if (tutorDni.isEmpty()) errores.add(new ErrorCampo("tutorDni", vacio));
        else if (!DNI.matcher(tutorDni).matches()) {
            errores.add(new ErrorCampo("tutorDni", "El DNI debe contener entre 7 y 8 numeros."));
        }
        if (tutorTelefono.isEmpty()) errores.add(new ErrorCampo("tutorTelefono", vacio));
        else if (!TELEFONO.matcher(tutorTelefono).matches()) {
            errores.add(new ErrorCampo("tutorTelefono", "El telefono del tutor debe tener un formato valido."));
        }
    }

    private static String errorNombre(String valor, String etiqueta) {
        if (valor.isEmpty()) return etiqueta + " es obligatorio.";
        if (valor.length() > 50) return etiqueta + " no puede superar los 50 caracteres.";
        return NOMBRE.matcher(valor).matches()
                ? null
                : etiqueta + " solo puede contener letras, espacios, guiones o apostrofes.";
    }

    private static String errorTelefono(String valor, String etiqueta) {
        if (valor.isEmpty()) return etiqueta + " es obligatorio.";
        return TELEFONO.matcher(valor).matches() ? null : etiqueta + " debe tener un formato valido.";
    }

    private static void agregar(List<ErrorCampo> errores, String campo, String mensaje) {
        if (mensaje != null) errores.add(new ErrorCampo(campo, mensaje));
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
