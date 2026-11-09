package com.example.core.ruta;

/** Ruta del catálogo con la cantidad de Checkpoints guardados en su Itinerario. */
public record Ruta(int id, String nombre, double altitudMaxima, String tipoTerreno,
        String dificultadTecnica, String dificultadFisica, boolean activa, int cantidadCheckpoints) {

    /** Mínimo de Checkpoints para que una Ruta deje de ser borrador. */
    public static final int CHECKPOINTS_MINIMOS = 2;

    /** Una Ruta cuyo Itinerario no tiene Checkpoints es borrador. */
    public boolean esBorrador() {
        return cantidadCheckpoints == 0;
    }

    /** Solo una Ruta activa con dos o más Checkpoints puede usarse para planificar una Salida. */
    public boolean puedeUsarseEnSalida() {
        return activa && cantidadCheckpoints >= CHECKPOINTS_MINIMOS;
    }
}
