package com.example.api.salida;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.core.salida.CasosDeUsoSalidas;

/** Cierre de una Salida planificada; las transiciones y la liberación de stock son reglas de core. */
@RestController
@RequestMapping("/api/salidas/{id}")
public class CierreSalidasController {

    private final CasosDeUsoSalidas salidas;

    public CierreSalidasController(CasosDeUsoSalidas salidas) {
        this.salidas = salidas;
    }

    @PostMapping("/realizada")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarRealizada(@PathVariable int id) {
        salidas.marcarRealizada(id);
    }

    @PostMapping("/cancelar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable int id) {
        salidas.cancelar(id);
    }
}
