package com.example.api.ruta;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.api.compartido.EstadoFiltro;
import com.example.core.ruta.CasosDeUsoRutas;
import com.example.core.ruta.Ruta;

@RestController
@RequestMapping("/api/rutas")
public class RutasController {

    private final CasosDeUsoRutas rutas;

    public RutasController(CasosDeUsoRutas rutas) {
        this.rutas = rutas;
    }

    @GetMapping
    public List<RutaDto> listar(@RequestParam(defaultValue = "activas") EstadoFiltro estado) {
        List<Ruta> resultado = switch (estado) {
            case activas -> rutas.listarActivas();
            case bajas -> rutas.listarDadasDeBaja();
            case todas -> rutas.listarTodas();
        };
        return resultado.stream().map(RutaDto::de).toList();
    }

    @GetMapping("/{id}")
    public RutaDto obtener(@PathVariable int id) {
        return RutaDto.de(rutas.obtener(id));
    }
}
