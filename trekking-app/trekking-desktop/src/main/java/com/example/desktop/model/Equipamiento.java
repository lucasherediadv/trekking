package com.example.desktop.model;

import java.util.List;

public class Equipamiento {

    public static final List<String> CATEGORIAS = List.of("Campamento", "Navegación", "Comunicación", "Seguridad",
            "Escalada", "Vestimenta", "Hidratación", "Primeros Auxilios", "Herramientas", "Transporte");
    public static final List<String> ESTADOS = List.of("Disponible", "Asignado", "En Reparación",
            "En Inspección", "Fuera de Servicio", "Extraviado");

    public static boolean categoriaValida(String categoria) {
        return categoria != null && CATEGORIAS.contains(categoria);
    }

    public static boolean estadoValido(String estado) {
        return estado != null && ESTADOS.contains(estado);
    }

    private int id;
    private String nombre;
    private String categoria;
    private int cantidad;
    private int cantidadReservada;
    private String estado;

    public Equipamiento(int id, String nombre, String categoria,
                         int cantidad, String estado) {
        this(id, nombre, categoria, cantidad, estado, 0);
    }

    public Equipamiento(int id, String nombre, String categoria,
                         int cantidad, String estado, int cantidadReservada) {
        this.id = id;
        this.nombre = nombre;
        this.categoria = categoria;
        this.cantidad = cantidad;
        this.estado = estado;
        this.cantidadReservada = cantidadReservada;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCategoria() {
        return categoria;
    }

    public int getCantidad() {
        return cantidad;
    }

    public String getEstado() {
        return estado;
    }

    public int getCantidadReservada() {
        return cantidadReservada;
    }

    public int getCantidadDisponible() {
        return cantidad - cantidadReservada;
    }
}
