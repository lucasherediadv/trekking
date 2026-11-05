package com.example.api.salida;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.api.inventario.ElementoDto;
import com.example.core.inventario.CasosDeUsoInventario;
import com.example.core.inventario.ElementoInventario;
import com.example.core.salida.CasosDeUsoEditarSalida;

/** Edición de una Salida planificada. Atomicidad y reglas son de core; aquí solo se traduce. */
@RestController
@RequestMapping("/api/salidas")
public class EdicionSalidasController {

    private final CasosDeUsoEditarSalida edicion;
    private final CasosDeUsoInventario inventario;

    public EdicionSalidasController(CasosDeUsoEditarSalida edicion, CasosDeUsoInventario inventario) {
        this.edicion = edicion;
        this.inventario = inventario;
    }

    @PutMapping("/{id}")
    public SalidaDto editar(@PathVariable int id, @RequestBody SolicitudEdicionSalidaDto solicitud) {
        return SalidaDto.de(edicion.editar(id, solicitud.aDatosEdicion()));
    }

    /** Elementos que se pueden ofrecer al editar; su reserva excluye la de la propia Salida. */
    @GetMapping("/{id}/elementos-para-editar")
    public List<ElementoDto> elementosParaEditar(@PathVariable int id) {
        List<ElementoInventario> elementos = edicion.elementosParaEditar(id);
        Set<Integer> activos = inventario.listarActivos().stream().map(ElementoInventario::id)
                .collect(Collectors.toSet());
        return elementos.stream().map(e -> {
            boolean activo = activos.contains(e.id());
            return new ElementoDto(e.id(), e.nombre(), e.categoria(), e.cantidad(), e.cantidadReservada(),
                    e.cantidadDisponible(), e.estado().name(), e.estado().etiqueta(), activo,
                    activo && !e.estado().impideAsignar());
        }).toList();
    }
}
