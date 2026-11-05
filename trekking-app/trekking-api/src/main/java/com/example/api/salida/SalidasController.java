package com.example.api.salida;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.core.salida.CasosDeUsoEditarSalida;
import com.example.core.salida.CasosDeUsoSalidas;

@RestController
@RequestMapping("/api/salidas")
public class SalidasController {

    private final CasosDeUsoSalidas salidas;
    private final CasosDeUsoEditarSalida edicion;

    public SalidasController(CasosDeUsoSalidas salidas, CasosDeUsoEditarSalida edicion) {
        this.salidas = salidas;
        this.edicion = edicion;
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
