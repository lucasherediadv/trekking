package com.example.core.itinerario;

import java.util.ArrayList;
import java.util.List;

import com.example.core.excepcion.ErrorCampo;

/** Reglas de validación de un Checkpoint. No tiene efectos: solo devuelve errores por campo. */
public final class ValidadorCheckpoint {

    private static final String MENSAJE_HORA = "Usa el formato de 24 horas HH:mm, por ejemplo 14:30.";

    private ValidadorCheckpoint() {
    }

    public static List<ErrorCampo> validar(Checkpoint checkpoint, double altitudMaxima) {
        return validar(checkpoint.nombre(), checkpoint.hora(), String.valueOf(checkpoint.latitud()),
                String.valueOf(checkpoint.longitud()), String.valueOf(checkpoint.altitud()), altitudMaxima);
    }

    /** Variante para formularios, donde las coordenadas y la altitud todavía son texto. */
    public static List<ErrorCampo> validar(String nombre, String hora, String latitud, String longitud,
            String altitud, double altitudMaxima) {
        List<ErrorCampo> errores = new ArrayList<>();
        if (nombre == null || nombre.isBlank()) {
            errores.add(new ErrorCampo("nombre", "Ingresa el nombre del Checkpoint."));
        }
        if (hora == null || !hora.matches("(?:[01]\\d|2[0-3]):[0-5]\\d")) {
            errores.add(new ErrorCampo("hora", MENSAJE_HORA));
        }
        agregar(errores, "latitud", rango(latitud, -90, 90, "La latitud debe estar entre -90 y 90."));
        agregar(errores, "longitud", rango(longitud, -180, 180, "La longitud debe estar entre -180 y 180."));
        agregar(errores, "altitud", rango(altitud, 0, altitudMaxima,
                "La altitud debe estar entre 0 y " + formatear(altitudMaxima) + " m."));
        return errores;
    }

    /** Convierte un número escrito en un formulario (admite coma decimal); solo válido tras {@link #validar}. */
    public static double parsearDecimal(String texto) {
        return Double.parseDouble(texto.trim().replace(',', '.'));
    }

    private static void agregar(List<ErrorCampo> errores, String campo, String mensaje) {
        if (mensaje != null) errores.add(new ErrorCampo(campo, mensaje));
    }

    private static String rango(String texto, double minimo, double maximo, String mensaje) {
        if (texto == null || texto.isBlank()) return "Este campo es obligatorio.";
        try {
            double numero = parsearDecimal(texto);
            return Double.isFinite(numero) && numero >= minimo && numero <= maximo ? null : mensaje;
        } catch (NumberFormatException e) {
            return "Ingresa un número decimal válido.";
        }
    }

    private static String formatear(double valor) {
        return valor == Math.rint(valor) && Math.abs(valor) < 1e15 ? String.valueOf((long) valor) : String.valueOf(valor);
    }
}
