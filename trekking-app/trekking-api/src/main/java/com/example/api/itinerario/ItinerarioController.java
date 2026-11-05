package com.example.api.itinerario;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.api.ruta.RutaDto;
import com.example.core.itinerario.CasosDeUsoItinerario;
import com.example.core.itinerario.Itinerario;

@RestController
@RequestMapping("/api/rutas/{rutaId}/itinerario")
public class ItinerarioController {

    private final CasosDeUsoItinerario itinerarios;

    public ItinerarioController(CasosDeUsoItinerario itinerarios) {
        this.itinerarios = itinerarios;
    }

    @GetMapping
    public List<CheckpointDto> obtener(@PathVariable int rutaId) {
        return itinerarios.obtener(rutaId).checkpoints().stream().map(CheckpointDto::de).toList();
    }

    /** Reemplaza el Itinerario completo; la posición en la lista desempata las horas iguales (ADR 0002). */
    @PutMapping
    public RutaDto guardar(@PathVariable int rutaId, @RequestBody List<CheckpointDto> checkpoints) {
        Itinerario itinerario = Itinerario.de(checkpoints.stream().map(CheckpointDto::aCheckpoint).toList());
        return RutaDto.de(itinerarios.guardar(rutaId, itinerario));
    }
}
