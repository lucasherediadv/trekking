package com.example.core.excepcion;

import java.util.List;

/** Los datos no cumplen las reglas de negocio; indica el error de cada campo afectado. */
public class ValidacionException extends ExcepcionNegocio {

    private final List<ErrorCampo> errores;

    public ValidacionException(List<ErrorCampo> errores) {
        super(errores.isEmpty()
                ? "Datos inválidos."
                : errores.stream().map(ErrorCampo::mensaje).reduce((a, b) -> a + " " + b).orElseThrow());
        this.errores = List.copyOf(errores);
    }

    public List<ErrorCampo> errores() {
        return errores;
    }
}
