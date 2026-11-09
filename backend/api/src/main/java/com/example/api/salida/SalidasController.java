package com.example.api.salida;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.api.config.PropiedadesApi;
import com.example.core.salida.CasosDeUsoConfirmarSalida;
import com.example.core.salida.CasosDeUsoEditarSalida;
import com.example.core.salida.CasosDeUsoSalidas;

@RestController
@RequestMapping("/api/salidas")
public class SalidasController {

    private final CasosDeUsoSalidas salidas;
    private final CasosDeUsoEditarSalida edicion;
    private final CasosDeUsoConfirmarSalida confirmacion;
    private final PropiedadesApi propiedades;

    public SalidasController(CasosDeUsoSalidas salidas, CasosDeUsoEditarSalida edicion,
            CasosDeUsoConfirmarSalida confirmacion, PropiedadesApi propiedades) {
        this.salidas = salidas;
        this.edicion = edicion;
        this.confirmacion = confirmacion;
        this.propiedades = propiedades;
    }

    /** La atomicidad y las reglas son de core; aquí solo se traduce el cuerpo. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalidaDto confirmar(@RequestBody SolicitudSalidaDto solicitud) {
        return SalidaDto.de(confirmacion.confirmar(solicitud.aDatosSalida(propiedades.maxFichaMedica().toBytes())));
    }

    @GetMapping
    public List<SalidaListadaDto> listar() {
        return salidas.listar().stream().map(SalidaListadaDto::de).toList();
    }

    @GetMapping("/{id}")
    public SalidaDto obtener(@PathVariable int id) {
        return SalidaDto.de(salidas.obtener(id));
    }

    @GetMapping("/{id}/copia-operativa")
    public CopiaOperativaDto copiaOperativa(@PathVariable int id) {
        return CopiaOperativaDto.de(edicion.copia(id));
    }
}
