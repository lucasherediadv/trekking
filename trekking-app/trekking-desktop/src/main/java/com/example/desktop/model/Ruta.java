package com.example.desktop.model;

import java.util.Locale;

import com.example.core.ruta.NombreRuta;

public class Ruta {

    public static String normalizarNombre(String nombre) {
        return NombreRuta.normalizar(nombre);
    }

    public static String formatearNombre(String nombre) {
        String normalizado = normalizarNombre(nombre).toLowerCase(Locale.ROOT);

        if (normalizado.isEmpty()) {
            return "";
        }

        String[] palabras = normalizado.split(" ");
        StringBuilder resultado = new StringBuilder();

        for (String palabra : palabras) {
            if (resultado.length() > 0) {
                resultado.append(" ");
            }

            resultado.append(
                    Character.toUpperCase(palabra.charAt(0))
            ).append(
                    palabra.substring(1)
            );
        }

        return resultado.toString();
    }

    public static boolean nombreValido(String nombre) {
        return normalizarNombre(nombre)
                .matches("[\\p{L}\\p{N}]+(?: [\\p{L}\\p{N}]+)*");
    }

    public static String claveNombre(String nombre) {
        return NombreRuta.clave(nombre);
    }

    private int id;
    private String nombre;
    private double altitudMaxima;
    private String tipoTerreno;
    private String dificultadTecnica;
    private String dificultadFisica;

    // Constructor para crear una ruta nueva
    public Ruta(
            String nombre,
            double altitudMaxima,
            String tipoTerreno,
            String dificultadTecnica,
            String dificultadFisica) {

        this.nombre = nombre;
        this.altitudMaxima = altitudMaxima;
        this.tipoTerreno = tipoTerreno;
        this.dificultadTecnica = dificultadTecnica;
        this.dificultadFisica = dificultadFisica;
    }

    // Constructor para recuperar una ruta existente desde SQLite
    public Ruta(
            int id,
            String nombre,
            double altitudMaxima,
            String tipoTerreno,
            String dificultadTecnica,
            String dificultadFisica) {

        this.id = id;
        this.nombre = nombre;
        this.altitudMaxima = altitudMaxima;
        this.tipoTerreno = tipoTerreno;
        this.dificultadTecnica = dificultadTecnica;
        this.dificultadFisica = dificultadFisica;
    }

    // GETTERS

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }


    public double getAltitudMaxima() {
        return altitudMaxima;
    }

    public String getTipoTerreno() {
        return tipoTerreno;
    }

    public String getDificultadTecnica() {
        return dificultadTecnica;
    }

    public String getDificultadFisica() {
        return dificultadFisica;
    }

    // SETTERS

    public void setId(int id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }


    public void setAltitudMaxima(double altitudMaxima) {
        this.altitudMaxima = altitudMaxima;
    }

    public void setTipoTerreno(String tipoTerreno) {
        this.tipoTerreno = tipoTerreno;
    }

    public void setDificultadTecnica(String dificultadTecnica) {
        this.dificultadTecnica = dificultadTecnica;
    }

    public void setDificultadFisica(String dificultadFisica) {
        this.dificultadFisica = dificultadFisica;
    }
}
