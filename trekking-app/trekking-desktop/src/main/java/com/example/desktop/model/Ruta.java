package com.example.desktop.model;

public class Ruta {

    /** Adapta la Ruta de core al modelo de presentación de la UI. */
    public static Ruta desde(com.example.core.ruta.Ruta ruta) {
        return new Ruta(ruta.id(), ruta.nombre(), ruta.altitudMaxima(), ruta.tipoTerreno(),
                ruta.dificultadTecnica(), ruta.dificultadFisica());
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
