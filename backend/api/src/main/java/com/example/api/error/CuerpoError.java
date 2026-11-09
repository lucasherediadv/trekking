package com.example.api.error;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Cuerpo JSON uniforme de todos los errores; los campos que no aplican se omiten. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CuerpoError(String codigo, String mensaje, List<ErrorCampoDto> errores, String elemento, Integer disponible) {

    public record ErrorCampoDto(String campo, String mensaje) {
    }

    public static CuerpoError de(String codigo, String mensaje) {
        return new CuerpoError(codigo, mensaje, null, null, null);
    }
}
