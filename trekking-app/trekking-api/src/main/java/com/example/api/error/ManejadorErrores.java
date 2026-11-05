package com.example.api.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.example.core.excepcion.EntidadNoEncontradaException;
import com.example.core.excepcion.ValidacionException;
import com.example.core.inventario.StockInsuficienteException;
import com.example.core.salida.SalidaNoEditableException;
import com.example.core.salida.TransicionInvalidaException;

/** Único punto de traducción de excepciones a respuestas HTTP. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(ValidacionException.class)
    ResponseEntity<CuerpoError> validacion(ValidacionException e) {
        var errores = e.errores().stream()
                .map(c -> new CuerpoError.ErrorCampoDto(c.campo(), c.mensaje()))
                .toList();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new CuerpoError("VALIDACION", e.getMessage(), errores, null, null));
    }

    @ExceptionHandler(EntidadNoEncontradaException.class)
    ResponseEntity<CuerpoError> noEncontrada(EntidadNoEncontradaException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(CuerpoError.de("NO_ENCONTRADO", e.getMessage()));
    }

    @ExceptionHandler({TransicionInvalidaException.class, SalidaNoEditableException.class})
    ResponseEntity<CuerpoError> conflicto(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(CuerpoError.de("CONFLICTO", e.getMessage()));
    }

    @ExceptionHandler(StockInsuficienteException.class)
    ResponseEntity<CuerpoError> stock(StockInsuficienteException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new CuerpoError("STOCK_INSUFICIENTE", e.getMessage(), null, e.elemento(), e.disponible()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<CuerpoError> peticionInvalida(Exception e) {
        return ResponseEntity.badRequest()
                .body(CuerpoError.de("PETICION_INVALIDA", "La petición está mal formada o tiene valores inválidos."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<CuerpoError> metodoNoPermitido(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(CuerpoError.de("METODO_NO_PERMITIDO", "El método no está permitido para este recurso."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<CuerpoError> rutaDesconocida(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(CuerpoError.de("NO_ENCONTRADO", "El recurso no existe."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<CuerpoError> metodoNoPermitido(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(CuerpoError.de("METODO_NO_PERMITIDO", "El método no está permitido para este recurso."));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<CuerpoError> inesperado(Exception e) {
        LOG.error("Error inesperado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CuerpoError.de("ERROR_INTERNO", "Ocurrió un error inesperado."));
    }
}
