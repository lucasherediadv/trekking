package com.example.core.excepcion;

/** Error de validación asociado a un campo del formulario o de la petición. */
public record ErrorCampo(String campo, String mensaje) {
}
