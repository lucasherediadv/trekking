package com.example.api.inventario;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.api.compartido.EstadoFiltro;
import com.example.core.inventario.CasosDeUsoInventario;
import com.example.core.inventario.DatosElemento;
import com.example.core.inventario.ElementoInventario;

@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    private final CasosDeUsoInventario inventario;

    public InventarioController(CasosDeUsoInventario inventario) {
        this.inventario = inventario;
    }

    @GetMapping
    public List<ElementoDto> listar(@RequestParam(defaultValue = "activas") EstadoFiltro estado) {
        return switch (estado) {
            case activas -> dtos(inventario.listarActivos(), true);
            case bajas -> dtos(inventario.listarDadosDeBaja(), false);
            case todas -> {
                var todos = dtos(inventario.listarActivos(), true);
                todos.addAll(dtos(inventario.listarDadosDeBaja(), false));
                yield todos;
            }
        };
    }

    @GetMapping("/asignables")
    public List<ElementoDto> asignables() {
        return dtos(inventario.listarAsignables(), true);
    }

    @GetMapping("/{id}")
    public ElementoDto obtener(@PathVariable int id) {
        return dto(inventario.obtener(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ElementoDto crear(@RequestBody DatosElemento datos) {
        return dto(inventario.alta(datos));
    }

    @PutMapping("/{id}")
    public ElementoDto editar(@PathVariable int id, @RequestBody DatosElemento datos) {
        return dto(inventario.editar(id, datos));
    }

    @PostMapping("/{id}/baja")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void darDeBaja(@PathVariable int id) {
        inventario.darDeBaja(id);
    }

    @PostMapping("/{id}/reactivar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reactivar(@PathVariable int id) {
        inventario.reactivar(id);
    }

    private static java.util.ArrayList<ElementoDto> dtos(List<ElementoInventario> elementos, boolean activo) {
        var resultado = new java.util.ArrayList<ElementoDto>();
        elementos.forEach(e -> resultado.add(ElementoDto.de(e, activo)));
        return resultado;
    }

    /** La vigencia no está en el elemento: se deduce de pertenecer al listado de activos. */
    private ElementoDto dto(ElementoInventario elemento) {
        boolean activo = inventario.listarActivos().stream().anyMatch(e -> e.id() == elemento.id());
        return ElementoDto.de(elemento, activo);
    }
}
