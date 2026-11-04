package com.example.core.ruta;

import java.util.ArrayList;
import java.util.List;

import com.example.core.excepcion.ErrorCampo;

/** Reglas de validación de una Ruta. No tiene efectos: solo devuelve errores por campo. */
public final class ValidadorRuta {

    public static final int LARGO_MAXIMO_NOMBRE = 100;
    public static final List<String> TIPOS_TERRENO = List.of("Rocoso", "Boscoso", "Sendero", "Mixto");
    public static final List<String> DIFICULTADES = List.of("Baja", "Media", "Alta");

    private ValidadorRuta() {
    }

    public static List<ErrorCampo> validar(DatosRuta datos) {
        return validar(datos.nombre(), String.valueOf(datos.altitudMaxima()), datos.tipoTerreno(),
                datos.dificultadTecnica(), datos.dificultadFisica());
    }

    /** Variante para formularios, donde la altitud todavía es el texto que se está escribiendo. */
    public static List<ErrorCampo> validar(String nombre, String altitudMaxima, String tipoTerreno,
            String dificultadTecnica, String dificultadFisica) {
        List<ErrorCampo> errores = new ArrayList<>();
        String errorNombre = errorNombre(nombre);
        if (errorNombre != null) errores.add(new ErrorCampo("nombre", errorNombre));
        String errorAltitud = errorAltitud(altitudMaxima);
        if (errorAltitud != null) errores.add(new ErrorCampo("altitudMaxima", errorAltitud));
        if (tipoTerreno == null || !TIPOS_TERRENO.contains(tipoTerreno)) {
            errores.add(new ErrorCampo("tipoTerreno", "Selecciona un tipo de terreno."));
        }
        if (dificultadTecnica == null || !DIFICULTADES.contains(dificultadTecnica)) {
            errores.add(new ErrorCampo("dificultadTecnica", "Selecciona una dificultad técnica."));
        }
        if (dificultadFisica == null || !DIFICULTADES.contains(dificultadFisica)) {
            errores.add(new ErrorCampo("dificultadFisica", "Selecciona una dificultad física."));
        }
        return errores;
    }

    /** Convierte el texto de altitud de un formulario (admite coma decimal); solo válido tras {@link #validar}. */
    public static double parsearAltitud(String texto) {
        return Double.parseDouble(texto.trim().replace(',', '.'));
    }

    private static String errorNombre(String nombre) {
        String normalizado = NombreRuta.normalizar(nombre);
        if (normalizado.isEmpty()) return "El nombre de la ruta es obligatorio.";
        if (normalizado.length() > LARGO_MAXIMO_NOMBRE) {
            return "El nombre no puede superar los " + LARGO_MAXIMO_NOMBRE + " caracteres.";
        }
        if (!normalizado.matches("[\\p{L}\\p{N}]+(?: [\\p{L}\\p{N}]+)*")) {
            return "El nombre solamente puede contener letras, números y espacios.";
        }
        return null;
    }

    private static String errorAltitud(String altitud) {
        String texto = altitud == null ? "" : altitud.trim();
        if (texto.isEmpty()) return "La altitud máxima es obligatoria.";
        try {
            double valor = parsearAltitud(texto);
            if (!Double.isFinite(valor)) return "Ingresa un valor numérico válido.";
            if (valor < 0) return "La altitud no puede ser negativa.";
            return null;
        } catch (NumberFormatException e) {
            return "Ingresa un valor numérico válido.";
        }
    }
}
