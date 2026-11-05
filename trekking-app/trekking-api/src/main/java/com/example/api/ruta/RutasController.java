package com.example.api.ruta;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    @PostMapping
    public ResponseEntity<RutaDto> crear(@RequestBody DatosRutaDto datos) {
        RutaDto creada = RutaDto.de(rutas.alta(datos.aDatos()));
        return ResponseEntity.created(URI.create("/api/rutas/" + creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    public RutaDto editar(@PathVariable int id, @RequestBody DatosRutaDto datos) {
        return RutaDto.de(rutas.editar(id, datos.aDatos()));
    }

    @PostMapping("/{id}/baja")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void darDeBaja(@PathVariable int id) {
        rutas.darDeBaja(id);
    }

    @PostMapping("/{id}/reactivar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reactivar(@PathVariable int id) {
        rutas.reactivar(id);
    }
}
