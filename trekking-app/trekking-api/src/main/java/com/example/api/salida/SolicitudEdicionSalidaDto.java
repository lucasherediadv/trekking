package com.example.api.salida;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.api.itinerario.CheckpointDto;
import com.example.core.excepcion.ErrorCampo;
import com.example.core.excepcion.ValidacionException;
import com.example.core.salida.DatosEdicionSalida;
import com.example.core.salida.DatosSalida.LineaEquipamiento;

/**
 * Cuerpo de {@code PUT /api/salidas/{id}}: reemplaza la Copia operativa vigente. Las listas ausentes se
 * toman como vacías y los booleanos ausentes como falsos; las reglas de negocio las valida core.
 */
public record SolicitudEdicionSalidaDto(LocalDate fecha, Integer rutaId, Boolean actualizarDesdeRuta,
        List<CheckpointDto> checkpoints, List<CheckpointDto> checkpointsNuevosParaRuta,
        List<LineaEquipamientoEdicion> equipamiento, List<Integer> participantes,
        List<Integer> refrescarEquipamiento, List<Integer> refrescarParticipantes) {

    public record LineaEquipamientoEdicion(int elementoId, int cantidad) {
    }

    /** @throws ValidacionException si falta un dato obligatorio del formato */
    public DatosEdicionSalida aDatosEdicion() {
        List<ErrorCampo> errores = new ArrayList<>();
        if (fecha == null) errores.add(new ErrorCampo("fecha", "La fecha de la Salida es obligatoria."));
        if (rutaId == null) errores.add(new ErrorCampo("rutaId", "La Ruta es obligatoria."));
        var participantesIds = new ArrayList<Integer>();
        for (Integer id : orVacia(participantes)) {
            if (id == null) errores.add(new ErrorCampo("participantes", "El id de Cliente es obligatorio."));
            else participantesIds.add(id);
        }
        if (!errores.isEmpty()) throw new ValidacionException(errores);

        return new DatosEdicionSalida(fecha, rutaId, Boolean.TRUE.equals(actualizarDesdeRuta),
                orVacia(checkpoints).stream().map(CheckpointDto::aCheckpoint).toList(),
                orVacia(checkpointsNuevosParaRuta).stream().map(CheckpointDto::aCheckpoint).toList(),
                orVacia(equipamiento).stream().map(l -> new LineaEquipamiento(l.elementoId(), l.cantidad())).toList(),
                participantesIds, conjunto(refrescarEquipamiento), conjunto(refrescarParticipantes));
    }

    private static Set<Integer> conjunto(List<Integer> ids) {
        return new HashSet<>(orVacia(ids));
    }

    private static <T> List<T> orVacia(List<T> lista) {
        return lista == null ? List.of() : lista;
    }
}
