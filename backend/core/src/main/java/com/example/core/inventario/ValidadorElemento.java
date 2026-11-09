package com.example.core.inventario;

import java.util.ArrayList;
import java.util.List;

import com.example.core.excepcion.ErrorCampo;

/** Reglas de validación de un elemento de inventario. No tiene efectos: solo devuelve errores por campo. */
public final class ValidadorElemento {

    public static final List<String> CATEGORIAS = List.of("Campamento", "Navegación", "Comunicación",
            "Seguridad", "Escalada", "Vestimenta", "Hidratación", "Primeros Auxilios", "Herramientas",
            "Transporte");

    private ValidadorElemento() {
    }

    public static List<ErrorCampo> validar(DatosElemento datos) {
        return validar(datos.nombre(), datos.categoria(), String.valueOf(datos.cantidad()), datos.estado());
    }

    /** Variante para formularios, donde la cantidad todavía es el texto que se está escribiendo. */
    public static List<ErrorCampo> validar(String nombre, String categoria, String cantidad,
            EstadoElemento estado) {
        List<ErrorCampo> errores = new ArrayList<>();
        if (nombre == null || nombre.isBlank()) {
            errores.add(new ErrorCampo("nombre", "El nombre es obligatorio."));
        }
        if (categoria == null || !CATEGORIAS.contains(categoria)) {
            errores.add(new ErrorCampo("categoria", "Seleccione una categoría."));
        }
        String errorCantidad = errorCantidad(cantidad);
        if (errorCantidad != null) errores.add(new ErrorCampo("cantidad", errorCantidad));
        if (estado == null) {
            errores.add(new ErrorCampo("estado", "Seleccione un estado."));
        }
        return errores;
    }

    private static String errorCantidad(String cantidad) {
        String texto = cantidad == null ? "" : cantidad.trim();
        if (texto.isEmpty()) return "La cantidad es obligatoria.";
        try {
            if (Integer.parseInt(texto) < 0) return "La cantidad total debe ser mayor o igual a cero.";
            return null;
        } catch (NumberFormatException e) {
            return "La cantidad debe ser un número entero mayor o igual a cero.";
        }
    }
}
