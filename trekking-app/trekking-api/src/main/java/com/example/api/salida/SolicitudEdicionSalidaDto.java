package com.example.api.salida;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.api.compartido.LineaEquipamientoDto;
import com.example.api.compartido.Solicitudes;
import com.example.api.itinerario.CheckpointDto;
import com.example.core.excepcion.ErrorCampo;
import com.example.core.excepcion.ValidacionException;
import com.example.core.salida.DatosEdicionSalida;

/**
 * Cuerpo de {@code PUT /api/salidas/{id}}: reemplaza la Copia operativa vigente. Las listas ausentes se
 * toman como vacías y los booleanos ausentes como falsos; las reglas de negocio las valida core.
 */
public record SolicitudEdicionSalidaDto(LocalDate fecha, Integer rutaId, Boolean actualizarDesdeRuta,
        List<CheckpointDto> checkpoints, List<CheckpointDto> checkpointsNuevosParaRuta,
        List<LineaEquipamientoDto> equipamiento, List<Integer> participantes,
        List<Integer> refrescarEquipamiento, List<Integer> refrescarParticipantes) {

    /** @throws ValidacionException si falta un dato obligatorio del formato */
    public DatosEdicionSalida aDatosEdicion() {
        List<ErrorCampo> errores = new ArrayList<>();
        Solicitudes.exigirFechaYRuta(fecha, rutaId, errores);
        var participantesIds = new ArrayList<Integer>();
        for (Integer id : Solicitudes.orVacia(participantes)) {
            if (id == null) errores.add(new ErrorCampo("participantes", "El id de Cliente es obligatorio."));
            else participantesIds.add(id);
        }
        if (!errores.isEmpty()) throw new ValidacionException(errores);

        return new DatosEdicionSalida(fecha, rutaId, Boolean.TRUE.equals(actualizarDesdeRuta),
                Solicitudes.orVacia(checkpoints).stream().map(CheckpointDto::aCheckpoint).toList(),
                Solicitudes.orVacia(checkpointsNuevosParaRuta).stream().map(CheckpointDto::aCheckpoint).toList(),
                Solicitudes.orVacia(equipamiento).stream().map(LineaEquipamientoDto::aLinea).toList(),
                participantesIds, conjunto(refrescarEquipamiento), conjunto(refrescarParticipantes));
    }

    private static Set<Integer> conjunto(List<Integer> ids) {
        return new HashSet<>(Solicitudes.orVacia(ids));
    }
}
